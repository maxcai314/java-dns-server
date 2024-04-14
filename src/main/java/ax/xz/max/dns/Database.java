package ax.xz.max.dns;

import java.sql.*;
import java.util.Arrays;

public class Database {
	public static void main(String[] args) throws ClassNotFoundException {
		Class.forName("org.sqlite.JDBC");
		try (
				Connection connection = DriverManager.getConnection("jdbc:sqlite:records.db");
				Statement statement = connection.createStatement()
		) {
			statement.setQueryTimeout(30);
			statement.executeUpdate("DROP TABLE IF EXISTS A_records");
			statement.executeUpdate("CREATE TABLE IF NOT EXISTS A_records (hostname TEXT PRIMARY KEY, address Binary(4))");
			statement.executeUpdate("INSERT INTO A_records VALUES ('example.com', X'7f000001')");
			statement.executeUpdate("INSERT INTO A_records VALUES ('example2.com', X'7f000001')");
			ResultSet rs = statement.executeQuery("SELECT * FROM A_records");

			while (rs.next()) {
				System.out.println("hostname = " + rs.getString("hostname"));
				System.out.println("address = " + Arrays.toString(rs.getBytes("address")));
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
}
