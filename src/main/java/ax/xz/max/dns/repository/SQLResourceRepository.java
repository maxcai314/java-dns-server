package ax.xz.max.dns.repository;

import ax.xz.max.dns.resource.*;
import org.sqlite.SQLiteDataSource;

import java.lang.foreign.SegmentAllocator;
import java.sql.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;

public class SQLResourceRepository implements ResourceRepository {
	private final SQLiteDataSource dataSource;
	private final SegmentAllocator allocator;
	public SQLResourceRepository(SegmentAllocator allocator) throws ResourceAccessException {
		this.allocator = allocator;
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
			statement.executeUpdate("CREATE TABLE IF NOT EXISTS records ( id Integer PRIMARY KEY, name Varchar(255) NOT NULL, class TEXT NOT NULL, time_to_live integer NOT NULL, data Varbinary(65535) NOT NULL )");
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
			statement.executeUpdate("CREATE TABLE IF NOT EXISTS records ( id Integer PRIMARY KEY, name Varchar(255) NOT NULL, class TEXT NOT NULL, time_to_live integer NOT NULL, data Varbinary(65535) NOT NULL )");
		} catch (SQLException e) {
			throw new ResourceAccessException("Failed to clear database", e);
		}
	}

	@Override
	public void insert(ResourceRecord record) throws ResourceAccessException {
		try (
				Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement("INSERT INTO records (name, class, time_to_live, data) VALUES (?, ?, ?, ?)");
		) {
			statement.setString(1, record.name().name());
			statement.setString(2, record.getClass().getSimpleName());
			statement.setInt(3, record.timeToLive());
			statement.setBytes(4, record.recordData(allocator).asByteBuffer().array());
			statement.executeUpdate();
		} catch (SQLException e) {
			throw new ResourceAccessException("Failed to insert record", e);
		}
	}

	@Override
	public void delete(ResourceRecord record) throws ResourceAccessException {

	}

	private ResourceRecord fromResultSet(ResultSet resultSet) throws SQLException {
		var name = new DomainName(resultSet.getString("name"));
		int timeToLive = resultSet.getInt("time_to_live");
		byte[] data = resultSet.getBytes("data");
		return switch (resultSet.getString("class")) {
			case "ARecord" -> ARecord.fromData(name, timeToLive, data);
			case "NSRecord" -> NSRecord.fromData(name, timeToLive, data);
			case "CNameRecord" -> CNameRecord.fromData(name, timeToLive, data);
			default -> throw new SQLException("Unknown record type");
		};
	}

	@Override
	public Collection<ResourceRecord> getAll() throws ResourceAccessException {
		try (
				Connection connection = dataSource.getConnection();
				Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery("SELECT * FROM records");
		) {
			Collection<ResourceRecord> records = new LinkedList<>();

			while (resultSet.next()) {
				records.add(fromResultSet(resultSet));
			}

			return records;
		} catch (SQLException e) {
			throw new ResourceAccessException("Failed to get all records", e);
		}
	}

	@Override
	public Collection<ResourceRecord> getAllByName(DomainName name) throws ResourceAccessException {
		try (
				Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement("SELECT * FROM records WHERE name = ?");
		) {
			statement.setString(1, name.name());
			ResultSet resultSet = statement.executeQuery();

			Collection<ResourceRecord> records = new LinkedList<>();

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
	public <T extends ResourceRecord> Collection<T> getAllByNameAndType(DomainName name, Class<T> clazz) throws ResourceAccessException {
		try (
				Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement("SELECT * FROM records WHERE name = ? AND class = ?");
		) {
			String className = clazz.getSimpleName();

			statement.setString(1, name.name());
			statement.setString(2, className);
			ResultSet resultSet = statement.executeQuery();

			Collection<T> records = new LinkedList<>();

			while (resultSet.next()) {
				if (!resultSet.getString("name").equals(name.name()))
					throw new ResourceAccessException("Name mismatch from database query");
				if (!resultSet.getString("class").equals(className))
					throw new ResourceAccessException("Class mismatch from database query");

				records.add((T) fromResultSet(resultSet));
			}

			return records;
		} catch (SQLException e) {
			throw new ResourceAccessException("Failed to get all records", e);
		}
	}

	public void printAll() throws ResourceAccessException {
		try (
				Connection connection = dataSource.getConnection();
				Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery("SELECT * FROM records");
		) {
			while (resultSet.next()) {
				System.out.println(resultSet.getString("name")
						+ " " + resultSet.getString("class")
						+ " " + resultSet.getInt("time_to_live")
						+ " " + Arrays.toString(resultSet.getBytes("data")));
			}
		} catch (SQLException e) {
			throw new ResourceAccessException("Failed to print all records", e);
		}
	}
}
