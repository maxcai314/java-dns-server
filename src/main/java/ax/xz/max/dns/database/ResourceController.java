package ax.xz.max.dns.database;

import org.sqlite.SQLiteDataSource;

import java.sql.*;
import java.util.Arrays;

public class ResourceController {
	private final SQLiteDataSource dataSource;
	public ResourceController() throws SQLException {
		dataSource = new SQLiteDataSource();
		dataSource.setUrl("jdbc:sqlite:records.db");
		initialize();
	}

	private void initialize() throws SQLException {
		try (
				Connection connection = dataSource.getConnection();
				Statement statement = connection.createStatement();
		) {
			statement.setQueryTimeout(30);
			statement.executeUpdate("CREATE TABLE IF NOT EXISTS A_records (hostname TEXT PRIMARY KEY, address Binary(4))");
		}
	}

	public void reset() throws SQLException {
		try (
				Connection connection = dataSource.getConnection();
				Statement statement = connection.createStatement();
		) {
			statement.setQueryTimeout(30);
			statement.executeUpdate("DROP TABLE IF EXISTS A_records");
			statement.executeUpdate("CREATE TABLE A_records (hostname TEXT PRIMARY KEY, address Binary(4))");
		}
	}

	public void insert(String hostname, byte[] address) throws SQLException {
		try (
				Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement("INSERT INTO A_records VALUES (?, ?)");
		) {
			statement.setQueryTimeout(30);
			statement.setString(1, hostname);
			statement.setBytes(2, address);
			statement.executeUpdate();
		}
	}

	public void delete(String hostname) throws SQLException {
		try (
				Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement("DELETE FROM A_records WHERE hostname = ?");
		) {
			statement.setQueryTimeout(30);
			statement.setString(1, hostname);
			statement.executeUpdate();
		}
	}

	public void printAll() throws SQLException {
		try (
				Connection connection = dataSource.getConnection();
				Statement statement = connection.createStatement();
		) {
			statement.setQueryTimeout(30);
			ResultSet results = statement.executeQuery("SELECT * FROM A_records");
			while (results.next()) {
				System.out.println("Hostname: " + results.getString("hostname") + " Address: " + Arrays.toString(results.getBytes("address")));
			}
		}
	}
}
