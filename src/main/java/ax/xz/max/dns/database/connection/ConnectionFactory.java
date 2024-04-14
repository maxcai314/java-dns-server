package ax.xz.max.dns.database.connection;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public @FunctionalInterface interface ConnectionFactory {
	Connection newConnection() throws SQLException;
}
