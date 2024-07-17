package ax.xz.max.dns.repository.connection;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.ReentrantLock;

public class ConnectionPool {
	private final ArrayBlockingQueue<Connection> availableConnections;
	private final Set<Connection> usedConnections = new HashSet<>();
	private int poolSize;
	private final int corePoolSize;
	private final int maxPoolSize;
	private volatile boolean isClosed = false;
	private final DataSource dataSource;

	public ConnectionPool(DataSource dataSource, int corePoolSize, int maxPoolSize) {
		availableConnections = new ArrayBlockingQueue<>(maxPoolSize);
		this.dataSource = dataSource;
		this.poolSize = 0;
		this.corePoolSize = corePoolSize;
		this.maxPoolSize = maxPoolSize;
	}

	private synchronized void addConnectionIfNeeded() throws SQLException {
		if (availableConnections.isEmpty() && poolSize < maxPoolSize) {
			availableConnections.add(dataSource.getConnection());
			poolSize++;
		}
	}

	public PooledConnection acquireConnection() throws InterruptedException, SQLException {
		if (isClosed) throw new IllegalStateException("Connection pool is closed");

		Connection connection;
		do {
			addConnectionIfNeeded();
			connection = availableConnections.take();
		} while (removeIfClosed(connection));

		usedConnections.add(connection);
		return new PooledConnection(connection);
	}

	/** Must be called using PooledConnection's delegate */
	private void releaseConnection(Connection connection) throws SQLException {
		if (isClosed) {
			removeConnection(connection);
			return; // just try to clean up, don't throw
		}

		if (!usedConnections.remove(connection)) throw new IllegalArgumentException("Connection not in use");
		if (removeIfClosed(connection)) return;
		if (removeSurplus(connection)) return;

		availableConnections.add(connection);
	}

	private boolean removeIfClosed(Connection connection) throws SQLException {
		if (connection.isClosed()) {
			poolSize--;
			return true;
		}
		return false;
	}

	private synchronized void removeConnection(Connection connection) throws SQLException {
		poolSize--;
		connection.close();
	}

	private synchronized boolean removeSurplus(Connection connection) throws SQLException {
		if (poolSize > corePoolSize) {
			poolSize--;
			connection.close();
			return true;
		}
		return false;
	}

	public synchronized void close() throws SQLException {
		if (isClosed) return;
		for (Connection connection : availableConnections) connection.close();
		for (Connection connection : usedConnections) connection.close();
		isClosed = true;
	}

	/** A wrapper around a Connection that returns it to the pool when closed */
	public class PooledConnection implements Connection, AutoCloseable {
		private volatile boolean released = false;
		private final Connection delegate;

		private PooledConnection(Connection delegate) {
			this.delegate = delegate;
		}

		private void throwIfReleased() {
			if (released) throw new IllegalStateException("Connection has already been released");
		}

		@Override
		public void close() throws SQLException {
			throwIfReleased();
			released = true;
			releaseConnection(delegate);
		}

		@Override
		public boolean isClosed() throws SQLException {
			throwIfReleased();
			return delegate.isClosed();
		}

		@Override
		public Statement createStatement() throws SQLException {
			throwIfReleased();
			return delegate.createStatement();
		}

		@Override
		public PreparedStatement prepareStatement(String sql) throws SQLException {
			throwIfReleased();
			return delegate.prepareStatement(sql);
		}

		@Override
		public CallableStatement prepareCall(String sql) throws SQLException {
			throwIfReleased();
			return delegate.prepareCall(sql);
		}

		@Override
		public String nativeSQL(String sql) throws SQLException {
			throwIfReleased();
			return delegate.nativeSQL(sql);
		}

		@Override
		public void setAutoCommit(boolean autoCommit) throws SQLException {
			throwIfReleased();
			delegate.setAutoCommit(autoCommit);
		}

		@Override
		public boolean getAutoCommit() throws SQLException {
			throwIfReleased();
			return delegate.getAutoCommit();
		}

		@Override
		public void commit() throws SQLException {
			throwIfReleased();
			delegate.commit();
		}

		@Override
		public void rollback() throws SQLException {
			throwIfReleased();
			delegate.rollback();
		}

		@Override
		public DatabaseMetaData getMetaData() throws SQLException {
			throwIfReleased();
			return delegate.getMetaData();
		}

		@Override
		public void setReadOnly(boolean readOnly) throws SQLException {
			throwIfReleased();
			delegate.setReadOnly(readOnly);
		}

		@Override
		public boolean isReadOnly() throws SQLException {
			throwIfReleased();
			return delegate.isReadOnly();
		}

		@Override
		public void setCatalog(String catalog) throws SQLException {
			throwIfReleased();
			delegate.setCatalog(catalog);
		}

		@Override
		public String getCatalog() throws SQLException {
			throwIfReleased();
			return delegate.getCatalog();
		}

		@Override
		public void setTransactionIsolation(int level) throws SQLException {
			throwIfReleased();
			delegate.setTransactionIsolation(level);
		}

		@Override
		public int getTransactionIsolation() throws SQLException {
			throwIfReleased();
			return delegate.getTransactionIsolation();
		}

		@Override
		public SQLWarning getWarnings() throws SQLException {
			throwIfReleased();
			return delegate.getWarnings();
		}

		@Override
		public void clearWarnings() throws SQLException {
			throwIfReleased();
			delegate.clearWarnings();
		}

		@Override
		public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
			throwIfReleased();
			return delegate.createStatement(resultSetType, resultSetConcurrency);
		}

		@Override
		public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
			throwIfReleased();
			return delegate.prepareStatement(sql, resultSetType, resultSetConcurrency);
		}

		@Override
		public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
			throwIfReleased();
			return delegate.prepareCall(sql, resultSetType, resultSetConcurrency);
		}

		@Override
		public Map<String, Class<?>> getTypeMap() throws SQLException {
			throwIfReleased();
			return delegate.getTypeMap();
		}

		@Override
		public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
			throwIfReleased();
			delegate.setTypeMap(map);
		}

		@Override
		public void setHoldability(int holdability) throws SQLException {
			throwIfReleased();
			delegate.setHoldability(holdability);
		}

		@Override
		public int getHoldability() throws SQLException {
			throwIfReleased();
			return delegate.getHoldability();
		}

		@Override
		public Savepoint setSavepoint() throws SQLException {
			throwIfReleased();
			return delegate.setSavepoint();
		}

		@Override
		public Savepoint setSavepoint(String name) throws SQLException {
			throwIfReleased();
			return delegate.setSavepoint(name);
		}

		@Override
		public void rollback(Savepoint savepoint) throws SQLException {
			throwIfReleased();
			delegate.rollback(savepoint);
		}

		@Override
		public void releaseSavepoint(Savepoint savepoint) throws SQLException {
			throwIfReleased();
			delegate.releaseSavepoint(savepoint);
		}

		@Override
		public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
			throwIfReleased();
			return delegate.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
		}

		@Override
		public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
			throwIfReleased();
			return delegate.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
		}

		@Override
		public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
			throwIfReleased();
			return delegate.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
		}

		@Override
		public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
			throwIfReleased();
			return delegate.prepareStatement(sql, autoGeneratedKeys);
		}

		@Override
		public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
			throwIfReleased();
			return delegate.prepareStatement(sql, columnIndexes);
		}

		@Override
		public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
			throwIfReleased();
			return delegate.prepareStatement(sql, columnNames);
		}

		@Override
		public Clob createClob() throws SQLException {
			throwIfReleased();
			return delegate.createClob();
		}

		@Override
		public Blob createBlob() throws SQLException {
			throwIfReleased();
			return delegate.createBlob();
		}

		@Override
		public NClob createNClob() throws SQLException {
			throwIfReleased();
			return delegate.createNClob();
		}

		@Override
		public SQLXML createSQLXML() throws SQLException {
			throwIfReleased();
			return delegate.createSQLXML();
		}

		@Override
		public boolean isValid(int timeout) throws SQLException {
			throwIfReleased();
			return delegate.isValid(timeout);
		}

		@Override
		public void setClientInfo(String name, String value) throws SQLClientInfoException {
			throwIfReleased();
			delegate.setClientInfo(name, value);
		}

		@Override
		public void setClientInfo(Properties properties) throws SQLClientInfoException {
			throwIfReleased();
			delegate.setClientInfo(properties);
		}

		@Override
		public String getClientInfo(String name) throws SQLException {
			throwIfReleased();
			return delegate.getClientInfo(name);
		}

		@Override
		public Properties getClientInfo() throws SQLException {
			throwIfReleased();
			return delegate.getClientInfo();
		}

		@Override
		public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
			throwIfReleased();
			return delegate.createArrayOf(typeName, elements);
		}

		@Override
		public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
			throwIfReleased();
			return delegate.createStruct(typeName, attributes);
		}

		@Override
		public void setSchema(String schema) throws SQLException {
			throwIfReleased();
			delegate.setSchema(schema);
		}

		@Override
		public String getSchema() throws SQLException {
			throwIfReleased();
			return delegate.getSchema();
		}

		@Override
		public void abort(Executor executor) throws SQLException {
			throwIfReleased();
			delegate.abort(executor);
		}

		@Override
		public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
			throwIfReleased();
			delegate.setNetworkTimeout(executor, milliseconds);
		}

		@Override
		public int getNetworkTimeout() throws SQLException {
			throwIfReleased();
			return delegate.getNetworkTimeout();
		}

		@Override
		public <T> T unwrap(Class<T> iface) throws SQLException {
			throwIfReleased();
			return delegate.unwrap(iface);
		}

		@Override
		public boolean isWrapperFor(Class<?> iface) throws SQLException {
			throwIfReleased();
			return delegate.isWrapperFor(iface);
		}
	}
}
