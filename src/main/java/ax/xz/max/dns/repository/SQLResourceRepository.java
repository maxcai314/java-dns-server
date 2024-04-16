package ax.xz.max.dns.repository;

import ax.xz.max.dns.resource.*;
import org.sqlite.SQLiteDataSource;

import java.lang.foreign.MemorySegment;
import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;

public class SQLResourceRepository implements ResourceRepository {
	private final SQLiteDataSource dataSource;
	public SQLResourceRepository() throws ResourceAccessException {
		dataSource = new SQLiteDataSource();
		dataSource.setUrl("jdbc:sqlite:records.db");
		initialize();
	}

	private void initialize() throws ResourceAccessException {
		try (
				Connection connection = dataSource.getConnection();
				Statement statement = connection.createStatement();
		) {
			statement.setQueryTimeout(30);
			statement.executeUpdate("CREATE TABLE IF NOT EXISTS records ( id Integer PRIMARY KEY, name Varchar(255) NOT NULL, type TEXT NOT NULL, time_to_live integer NOT NULL, data Varbinary(65535) NOT NULL )");
		} catch (SQLException e) {
			throw new ResourceAccessException("Failed to initialize database", e);
		}
	}

	@Override
	public void clear() throws ResourceAccessException {
		try (
				Connection connection = dataSource.getConnection();
				Statement statement = connection.createStatement();
		) {
			statement.setQueryTimeout(30);
			statement.executeUpdate("DROP TABLE IF EXISTS records");
			statement.executeUpdate("CREATE TABLE IF NOT EXISTS records ( id Integer PRIMARY KEY, name Varchar(255) NOT NULL, type TEXT NOT NULL, time_to_live integer NOT NULL, data Varbinary(65535) NOT NULL )");
		} catch (SQLException e) {
			throw new ResourceAccessException("Failed to clear database", e);
		}
	}

	@Override
	public void insert(ResourceRecord record) throws ResourceAccessException {
		try (
				Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement("INSERT INTO records (name, type, time_to_live, data) VALUES (?, ?, ?, ?)");
		) {
			statement.setString(1, record.name().name());
			statement.setString(2, record.getClass().getSimpleName());
			statement.setInt(3, record.timeToLive());
			statement.setBytes(4, record.recordData().asByteBuffer().array());
			statement.executeUpdate();
		} catch (SQLException e) {
			throw new ResourceAccessException("Failed to insert record", e);
		}
	}

	@Override
	public List<ResourceRecord> delete(ResourceRecord record) throws ResourceAccessException {
		try (
				Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement("DELETE FROM records WHERE name = ? AND type = ? AND time_to_live = ? AND data = ? RETURNING *");
		) {
			statement.setString(1, record.name().name());
			statement.setString(2, record.getClass().getSimpleName());
			statement.setInt(3, record.timeToLive());
			statement.setBytes(4, record.recordData().asByteBuffer().array());
			ResultSet resultSet = statement.executeQuery();

			List<ResourceRecord> records = new LinkedList<>();

			while (resultSet.next()) {
				records.add(fromResultSet(resultSet));
			}

			return records;
		} catch (SQLException e) {
			throw new ResourceAccessException("Failed to delete record", e);
		}
	}

	@Override
	public List<ResourceRecord> deleteAllByName(ResourceRecord record) throws ResourceAccessException {
		try (
				Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement("DELETE FROM records WHERE name = ? RETURNING *");
		) {
			statement.setString(1, record.name().name());
			ResultSet resultSet = statement.executeQuery();

			List<ResourceRecord> records = new LinkedList<>();

			while (resultSet.next()) {
				records.add(fromResultSet(resultSet));
			}

			return records;
		} catch (SQLException e) {
			throw new ResourceAccessException("Failed to delete record", e);
		}
	}

	@Override
	public <T extends ResourceRecord> List<T> deleteAllByNameAndType(DomainName name, Class<T> type) throws ResourceAccessException {
		try (
				Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement("DELETE FROM records WHERE name = ? AND type = ? RETURNING *");
		) {
			if (type.getSimpleName().equals("RecordData"))
				throw new IllegalArgumentException("Invalid type name: " + type.getSimpleName());

			statement.setString(1, name.name());
			statement.setString(2, type.getSimpleName());
			ResultSet resultSet = statement.executeQuery();

			List<T> records = new LinkedList<>();

			while (resultSet.next()) {
				records.add((T) fromResultSet(resultSet));
			}

			return records;
		} catch (SQLException e) {
			throw new ResourceAccessException("Failed to delete record", e);
		}
	}

	@Override
	public <T extends ResourceRecord> List<T> getAllByType(Class<T> type) throws ResourceAccessException {
		try (
				Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement("SELECT * FROM records WHERE type = ?");
		) {
			if (type.getSimpleName().equals("RecordData"))
				throw new IllegalArgumentException("Invalid type name: " + type.getSimpleName());

			statement.setString(1, type.getSimpleName());
			ResultSet resultSet = statement.executeQuery();

			List<T> records = new LinkedList<>();

			while (resultSet.next()) {
				records.add((T) fromResultSet(resultSet));
			}

			return records;
		} catch (SQLException e) {
			throw new ResourceAccessException("Failed to get all records", e);
		}
	}

	@Override
	public <T extends ResourceRecord> List<T> deleteAllByType(Class<T> type) throws ResourceAccessException {
		try (
				Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement("DELETE FROM records WHERE type = ? RETURNING *");
		) {
			if (type.getSimpleName().equals("RecordData"))
				throw new IllegalArgumentException("Invalid type name: " + type.getSimpleName());

			statement.setString(1, type.getSimpleName());
			ResultSet resultSet = statement.executeQuery();

			List<T> records = new LinkedList<>();

			while (resultSet.next()) {
				records.add((T) fromResultSet(resultSet));
			}

			return records;
		} catch (SQLException e) {
			throw new ResourceAccessException("Failed to delete record", e);
		}
	}

	private ResourceRecord fromResultSet(ResultSet resultSet) throws SQLException {
		var name = new DomainName(resultSet.getString("name"));
		int timeToLive = resultSet.getInt("time_to_live");
		MemorySegment data = MemorySegment.ofArray(resultSet.getBytes("data"));
		return switch (resultSet.getString("type")) {
			case "ARecord" -> ARecord.fromData(name, timeToLive, data);
			case "NSRecord" -> NSRecord.fromData(name, timeToLive, data);
			case "CNameRecord" -> CNameRecord.fromData(name, timeToLive, data);
			default -> throw new SQLException("Unknown record type");
		};
	}

	@Override
	public List<ResourceRecord> getAll() throws ResourceAccessException {
		try (
				Connection connection = dataSource.getConnection();
				Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery("SELECT * FROM records");
		) {
			List<ResourceRecord> records = new LinkedList<>();

			while (resultSet.next()) {
				records.add(fromResultSet(resultSet));
			}

			return records;
		} catch (SQLException e) {
			throw new ResourceAccessException("Failed to get all records", e);
		}
	}

	@Override
	public List<ResourceRecord> getAllByName(DomainName name) throws ResourceAccessException {
		try (
				Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement("SELECT * FROM records WHERE name = ?");
		) {
			statement.setString(1, name.name());
			ResultSet resultSet = statement.executeQuery();

			List<ResourceRecord> records = new LinkedList<>();

			while (resultSet.next()) {
				if (!resultSet.getString("name").equals(name.name()))
					throw new ResourceAccessException("Name mismatch from database query");

				records.add(fromResultSet(resultSet));
			}

			return records;
		} catch (SQLException e) {
			throw new ResourceAccessException("Failed to get all records", e);
		}
	}

	@Override
	public <T extends ResourceRecord> List<T> getAllByNameAndType(DomainName name, Class<T> type) throws ResourceAccessException {
		try (
				Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement("SELECT * FROM records WHERE name = ? AND type = ?");
		) {
			String typeName = type.getSimpleName();
			if (typeName.equals("RecordData"))
				throw new IllegalArgumentException("Invalid type name: " + typeName);

			statement.setString(1, name.name());
			statement.setString(2, typeName);
			ResultSet resultSet = statement.executeQuery();

			List<T> records = new LinkedList<>();

			while (resultSet.next()) {
				if (!resultSet.getString("name").equals(name.name()))
					throw new ResourceAccessException("Name mismatch from database query");
				if (!resultSet.getString("type").equals(typeName))
					throw new ResourceAccessException("Class mismatch from database query");

				records.add((T) fromResultSet(resultSet));
			}

			return records;
		} catch (SQLException e) {
			throw new ResourceAccessException("Failed to get all records", e);
		}
	}
}
