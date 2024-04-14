package ax.xz.max.dns;

import ax.xz.max.dns.database.ResourceController;
import java.sql.*;

public class Database {
	public static void main(String[] args) throws SQLException {
		ResourceController controller = new ResourceController();
		controller.reset();
		controller.insert("example.com", new byte[]{127, 0, 0, 1});
		controller.insert("example.net", new byte[]{127, 0, 0, 2});
		controller.insert("example.org", new byte[]{127, 0, 0, 3});
		controller.delete("example.net");
		controller.printAll();
	}
}
