package ax.xz.max.dns.database.connection;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * A service that will provide a Connection to a database, but must be returned after use.
 */
public interface ConnectionService extends AutoCloseable {
	/**
	 * Get a Connection to the database.
	 *
	 * @return a Connection to the database
	 */
	Connection acquireConnection() throws SQLException, InterruptedException;

	/**
	 * Release a Connection back to the ConnectionService.
	 *
	 * @param connection the Connection to release
	 */
	void releaseConnection(Connection connection) throws SQLException;

	/**
	 * Close the ConnectionService, releasing any resources it holds.
	 */
	@Override
	void close() throws SQLException;

}
