package ax.xz.max.dns.repository.connection;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public @FunctionalInterface interface ConnectionFactory {
	Connection newConnection() throws SQLException;

	static ConnectionFactory of(DataSource dataSource) {
		return dataSource::getConnection;
	}
}
