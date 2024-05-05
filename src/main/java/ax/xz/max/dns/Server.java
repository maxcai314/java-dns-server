package ax.xz.max.dns;

import ax.xz.max.dns.repository.SQLResourceRepository;
import ax.xz.max.dns.resource.*;
import ax.xz.max.dns.server.DNSServer;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;

public class Server {
	public static void main(String[] args) throws IOException {
		SQLResourceRepository controller = new SQLResourceRepository();
		controller.clear();

		var com = new ARecord(
				new DomainName("testing.xz.ax"),
				10,
				Inet4Address.ofLiteral("65.108.126.123")
		);

		var com6 = new AAAARecord(
				new DomainName("testing.xz.ax"),
				10,
				(Inet6Address) Inet6Address.ofLiteral("2a01:4f9:6b:15ce::2")
		);

		var www = new CNameRecord(
				new DomainName("www.testing.xz.ax"),
				10,
				new DomainName("testing.xz.ax")
		);

		var ns = new NSRecord(
				new DomainName("testing.xz.ax"),
				10,
				new DomainName("ns.xz.ax")
		);

		controller.insert(com);
		controller.insert(com6);
		controller.insert(www);
		controller.insert(ns);

		try (var server = new DNSServer(controller)) {
			System.in.read();
		}
	}
}
