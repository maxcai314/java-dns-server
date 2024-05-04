package ax.xz.max.dns.repository;

import ax.xz.max.dns.resource.*;
import org.sqlite.SQLiteDataSource;

import java.lang.foreign.MemorySegment;
import java.sql.*;
import java.util.List;
import java.util.LinkedList;

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
			statement.executeUpdate("CREATE TABLE IF NOT EXISTS records ( id Integer PRIMARY KEY, name Varbinary(255) NOT NULL, type Integer NOT NULL, time_to_live integer NOT NULL, data Varbinary(65535) NOT NULL )");
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
			statement.executeUpdate("CREATE TABLE IF NOT EXISTS records ( id Integer PRIMARY KEY, name Varbinary(255) NOT NULL, type Integer NOT NULL, time_to_live integer NOT NULL, data Varbinary(65535) NOT NULL )");
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
			statement.setBytes(1, record.name().bytes());
			statement.setShort(2, record.type());
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
			statement.setBytes(1, record.name().bytes());
			statement.setShort(2, record.type());
			statement.setInt(3, record.timeToLive());
			statement.setBytes(4, record.recordData().asByteBuffer().array());
			ResultSet resultSet = statement.executeQuery();

			List<ResourceRecord> records = new LinkedList<>();

			while (resultSet.next())
				records.add(ResourceRecord.fromData(
						DomainName.fromData(MemorySegment.ofArray(resultSet.getBytes("name"))),
						resultSet.getShort("type"),
						resultSet.getInt("time_to_live"),
						MemorySegment.ofArray(resultSet.getBytes("data"))
				));

			return records;
		} catch (SQLException e) {
			throw new ResourceAccessException("Failed to delete record", e);
		}
	}

	@Override
	public List<ResourceRecord> deleteAllByName(DomainName name) throws ResourceAccessException {
		try (
				Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement("DELETE FROM records WHERE name = ? RETURNING type, time_to_live, data");
		) {
			statement.setBytes(1, name.bytes());
			ResultSet resultSet = statement.executeQuery();

			List<ResourceRecord> records = new LinkedList<>();

			while (resultSet.next())
				records.add(ResourceRecord.fromData(
						name,
						resultSet.getShort("type"),
						resultSet.getInt("time_to_live"),
						MemorySegment.ofArray(resultSet.getBytes("data"))
				));

			return records;
		} catch (SQLException e) {
			throw new ResourceAccessException("Failed to delete record", e);
		}
	}

	@Override
	public List<ResourceRecord> deleteAllByNameAndType(DomainName name, short type) throws ResourceAccessException {
		try (
				Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement("DELETE FROM records WHERE name = ? AND type = ? RETURNING time_to_live, data");
		) {
			statement.setBytes(1, name.bytes());
			statement.setShort(2, type);
			ResultSet resultSet = statement.executeQuery();

			List<ResourceRecord> records = new LinkedList<>();

			while (resultSet.next())
				records.add(ResourceRecord.fromData(
						name,
						type,
						resultSet.getInt("time_to_live"),
						MemorySegment.ofArray(resultSet.getBytes("data"))
				));

			return records;
		} catch (SQLException e) {
			throw new ResourceAccessException("Failed to delete record", e);
		}
	}

	@Override
	public List<ResourceRecord> getAllByType(short type) throws ResourceAccessException {
		try (
				Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement("SELECT name, time_to_live, data FROM records WHERE type = ?");
		) {
			statement.setShort(1, type);
			ResultSet resultSet = statement.executeQuery();

			List<ResourceRecord> records = new LinkedList<>();

			while (resultSet.next())
				records.add(ResourceRecord.fromData(
						DomainName.fromData(MemorySegment.ofArray(resultSet.getBytes("name"))),
						type,
						resultSet.getInt("time_to_live"),
						MemorySegment.ofArray(resultSet.getBytes("data"))
				));

			return records;
		} catch (SQLException e) {
			throw new ResourceAccessException("Failed to get all records", e);
		}
	}

	@Override
	public List<ResourceRecord> deleteAllByType(short type) throws ResourceAccessException {
		try (
				Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement("DELETE FROM records WHERE type = ? RETURNING name, time_to_live, data");
		) {
			statement.setShort(1, type);
			ResultSet resultSet = statement.executeQuery();

			List<ResourceRecord> records = new LinkedList<>();

			while (resultSet.next())
				records.add(ResourceRecord.fromData(
						DomainName.fromData(MemorySegment.ofArray(resultSet.getBytes("name"))),
						type,
						resultSet.getInt("time_to_live"),
						MemorySegment.ofArray(resultSet.getBytes("data"))
				));

			return records;
		} catch (SQLException e) {
			throw new ResourceAccessException("Failed to delete record", e);
		}
	}

	@Override
	public List<ResourceRecord> getAll() throws ResourceAccessException {
		try (
				Connection connection = dataSource.getConnection();
				Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery("SELECT * FROM records");
		) {
			List<ResourceRecord> records = new LinkedList<>();

			while (resultSet.next())
				records.add(ResourceRecord.fromData(
						DomainName.fromData(MemorySegment.ofArray(resultSet.getBytes("name"))),
						resultSet.getShort("type"),
						resultSet.getInt("time_to_live"),
						MemorySegment.ofArray(resultSet.getBytes("data"))
				));

			return records;
		} catch (SQLException e) {
			throw new ResourceAccessException("Failed to get all records", e);
		}
	}

	@Override
	public List<ResourceRecord> getAllByName(DomainName name) throws ResourceAccessException {
		try (
				Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement("SELECT type, time_to_live, data FROM records WHERE name = ?");
		) {
			statement.setBytes(1, name.bytes());
			ResultSet resultSet = statement.executeQuery();

			List<ResourceRecord> records = new LinkedList<>();

			while (resultSet.next())
				records.add(ResourceRecord.fromData(
						name,
						resultSet.getShort("type"),
						resultSet.getInt("time_to_live"),
						MemorySegment.ofArray(resultSet.getBytes("data"))
				));

			return records;
		} catch (SQLException e) {
			throw new ResourceAccessException("Failed to get all records", e);
		}
	}

	@Override
	public List<ResourceRecord> getAllByNameAndType(DomainName name, short type) throws ResourceAccessException {
		try (
				Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement("SELECT time_to_live, data FROM records WHERE name = ? AND type = ?");
		) {
			statement.setBytes(1, name.bytes());
			statement.setShort(2, type);
			ResultSet resultSet = statement.executeQuery();

			List<ResourceRecord> records = new LinkedList<>();

			while (resultSet.next())
				records.add(ResourceRecord.fromData(
						name,
						type,
						resultSet.getInt("time_to_live"),
						MemorySegment.ofArray(resultSet.getBytes("data"))
				));

			return records;
		} catch (SQLException e) {
			throw new ResourceAccessException("Failed to get all records", e);
		}
	}
}
