package ax.xz.max.dns.repository;

import ax.xz.max.dns.resource.ARecord;
import org.sqlite.SQLiteDataSource;

import java.sql.*;
import java.util.Arrays;
import java.util.Optional;

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
			statement.executeUpdate("CREATE TABLE IF NOT EXISTS A_records (hostname TEXT PRIMARY KEY, address Binary(4))");
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
			statement.executeUpdate("DROP TABLE IF EXISTS A_records");
			statement.executeUpdate("CREATE TABLE A_records (hostname TEXT PRIMARY KEY, address Binary(4))");
		} catch (SQLException e) {
			throw new ResourceAccessException("Failed to clear database", e);
		}
	}

	@Override
	public void insertARecord(ARecord aRecord) throws ResourceAccessException {
		try (
				Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement("INSERT INTO A_records VALUES (?, ?)");
		) {
			statement.setQueryTimeout(30);
			statement.setString(1, aRecord.domain());
			statement.setBytes(2, aRecord.ip());
			statement.executeUpdate();
		} catch (SQLException e) {
			throw new ResourceAccessException("Failed to insert record", e);
		}
	}

	@Override
	public void deleteARecord(String hostname) throws ResourceAccessException {
		try (
				Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement("DELETE FROM A_records WHERE hostname = ?");
		) {
			statement.setQueryTimeout(30);
			statement.setString(1, hostname);
			statement.executeUpdate();
		} catch (SQLException e) {
			throw new ResourceAccessException("Failed to delete record", e);
		}
	}

	@Override
	public Optional<ARecord> getARecord(String hostname) throws ResourceAccessException {
		try (
				Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement("SELECT * FROM A_records WHERE hostname = ?");
		) {
			statement.setQueryTimeout(30);
			statement.setString(1, hostname);
			ResultSet results = statement.executeQuery();
			if (results.next())
				return Optional.of(new ARecord(results.getString("hostname"), results.getBytes("address")));
			else
				return Optional.empty();
		} catch (SQLException e) {
			throw new ResourceAccessException("Failed to get record", e);
		}
	}

	public void printAll() throws ResourceAccessException {
		try (
				Connection connection = dataSource.getConnection();
				Statement statement = connection.createStatement();
		) {
			statement.setQueryTimeout(30);
			ResultSet results = statement.executeQuery("SELECT * FROM A_records");
			while (results.next()) {
				System.out.println("Hostname: " + results.getString("hostname") + " Address: " + Arrays.toString(results.getBytes("address")));
			}
		} catch (SQLException e) {
			throw new ResourceAccessException("Failed to print records", e);
		}
	}
}
