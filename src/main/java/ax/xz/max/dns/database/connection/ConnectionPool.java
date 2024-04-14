package ax.xz.max.dns.database.connection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

public class ConnectionPool implements ConnectionService {
	private final Stack<Connection> availableConnections = new Stack<>();
	private final Set<Connection> usedConnections = new HashSet<>();
	private int poolSize;
	private final int corePoolSize;
	private final int maxPoolSize;
	private boolean isClosed = false;
	private final ConnectionFactory connectionFactory;

	public ConnectionPool(ConnectionFactory connectionFactory, int corePoolSize, int maxPoolSize) throws SQLException {
		this.connectionFactory = connectionFactory;
		this.poolSize = 0;
		this.corePoolSize = corePoolSize;
		this.maxPoolSize = maxPoolSize;
		for (int i = 0; i < corePoolSize; i++) {
			addNewConnection();
		}
	}

	private void addNewConnection() throws SQLException {
		availableConnections.push(connectionFactory.newConnection());
		poolSize++;
	}

	@Override
	public synchronized Connection acquireConnection() throws InterruptedException, SQLException {
		if (isClosed) throw new IllegalStateException("Connection pool is closed");
		if (availableConnections.isEmpty()) {
			if (poolSize < maxPoolSize) addNewConnection();
			else do wait(); while (availableConnections.isEmpty());
		}

		Connection connection = availableConnections.pop();
		usedConnections.add(connection);
		return connection;
	}

	@Override
	public synchronized void releaseConnection(Connection connection) throws SQLException {
		if (isClosed) throw new IllegalStateException("Connection pool is closed");
		if (!usedConnections.remove(connection)) throw new IllegalArgumentException("Connection not in use");
		if (poolSize > corePoolSize || connection.isClosed()) removeConnection(connection);
		else availableConnections.push(connection);
	}

	private void removeConnection(Connection connection) throws SQLException {
		poolSize--;
		connection.close();
	}

	@Override
	public void close() throws SQLException {
		for (Connection connection : availableConnections) connection.close();
		for (Connection connection : usedConnections) connection.close();
		isClosed = true;
	}
}
