package ax.xz.max.dns.repository.connection;

import java.sql.Connection;
import java.sql.SQLException;

public @FunctionalInterface interface ConnectionFactory {
	Connection newConnection() throws SQLException;
}
