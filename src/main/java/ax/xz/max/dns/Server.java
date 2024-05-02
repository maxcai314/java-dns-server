package ax.xz.max.dns;

import ax.xz.max.dns.repository.SQLResourceRepository;
import ax.xz.max.dns.resource.*;
import ax.xz.max.dns.server.DNSServer;

import java.io.IOException;
import java.net.Inet4Address;

public class Server {
	public static void main(String[] args) throws IOException {
		SQLResourceRepository controller = new SQLResourceRepository();
		controller.clear();

		var com = new ARecord(
				new DomainName("testing.xz.ax"),
				10,
				Inet4Address.ofLiteral("65.108.126.123")
		);

		var www = new CNameRecord(
				new DomainName("www.testing.xz.ax"),
				10,
				new DomainName("testing.xz.ax")
		);

		var subdomain = new CNameRecord(
				new DomainName("subdomain.testing.xz.ax"),
				10,
				new DomainName("testing.xz.ax")
		);

		controller.insert(com);
		controller.insert(www);
		controller.insert(subdomain);

		try (var server = new DNSServer(controller)) {
			System.in.read();
		}
	}
}
