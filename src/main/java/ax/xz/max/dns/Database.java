package ax.xz.max.dns;

import ax.xz.max.dns.database.SQLResourceRepository;
import ax.xz.max.dns.resource.ARecord;

import java.sql.*;

public class Database {
	public static void main(String[] args) throws SQLException {
		SQLResourceRepository controller = new SQLResourceRepository();
		controller.clear();
		controller.insertARecord(new ARecord("example.com", new byte[]{127, 0, 0, 1}));
		controller.insertARecord(new ARecord("example.net", new byte[]{127, 0, 0, 2}));
		controller.insertARecord(new ARecord("example.org", new byte[]{127, 0, 0, 3}));
		controller.deleteARecord("example.net");
		controller.printAll();
		System.out.println(controller.getARecord("example.com"));
		System.out.println(controller.getARecord("example.net")); // empty
	}
}
