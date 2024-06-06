package ax.xz.max.dns;

import ax.xz.max.dns.repository.CachingResourceRepository;
import ax.xz.max.dns.repository.SQLResourceRepository;
import ax.xz.max.dns.resource.*;
import ax.xz.max.dns.server.DNSServer;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Set;

public class Server {
	public static void main(String[] args) throws InterruptedException {
		try (CachingResourceRepository controller = CachingResourceRepository.of(new SQLResourceRepository())) {
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

			var ns4 = new ARecord(
					new DomainName("ns.xz.ax"),
					10,
					Inet4Address.ofLiteral("65.108.126.123")
			);

			var ns6 = new AAAARecord(
					new DomainName("ns.xz.ax"),
					10,
					(Inet6Address) Inet6Address.ofLiteral("2a01:4f9:6b:15ce::2")
			);

			controller.insert(com);
			controller.insert(com6);
			controller.insert(www);
			controller.insert(ns);
			controller.insert(ns4);
			controller.insert(ns6);

			controller.flushCache();

			var localAddresses = Set.of(
					new InetSocketAddress(InetAddress.ofLiteral("65.108.126.123"), 53),
					new InetSocketAddress(InetAddress.ofLiteral("2a01:4f9:6b:15ce::2"), 53)
			);

			try (var server = new DNSServer(controller, Thread.ofVirtual().factory(), localAddresses)) {
				Thread.sleep(Long.MAX_VALUE);
			}
		}
	}
}
