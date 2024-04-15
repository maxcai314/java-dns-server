package ax.xz.max.dns;

import ax.xz.max.dns.repository.SQLResourceRepository;
import ax.xz.max.dns.resource.ARecord;
import ax.xz.max.dns.resource.CNameRecord;
import ax.xz.max.dns.resource.DomainName;
import ax.xz.max.dns.resource.NSRecord;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.net.Inet4Address;

public class Database {
	public static void main(String[] args) {
		SegmentAllocator allocator = SegmentAllocator.prefixAllocator(MemorySegment.ofArray(new byte[65535]));
		SQLResourceRepository controller = new SQLResourceRepository(allocator);
		controller.clear();

		var com = new ARecord(new DomainName("example.com"), 10, Inet4Address.ofLiteral("127.0.0.1"));
		var net = new NSRecord(new DomainName("example.net"), 60, new DomainName("ns.example.net"));
		var wwwOrg = new CNameRecord(new DomainName("www.example.org"), 500, new DomainName("example.org"));
		var org = new ARecord(new DomainName("example.org"), 500, Inet4Address.ofLiteral("127.0.0.1"));
		var org2 = new ARecord(new DomainName("example.org"), 500, Inet4Address.ofLiteral("127.0.0.2"));

		controller.insert(com);
		controller.insert(net);
		controller.insert(wwwOrg);
		controller.insert(org);
		controller.insert(org2);
		controller.delete(net); // not implemented!

		System.out.println("printing all:");
		controller.getAll().forEach(System.out::println);

		System.out.println("\nprinting all by name:");
		System.out.println(controller.getAllByName(new DomainName("example.com")));
		System.out.println(controller.getAllByName(new DomainName("example.org"))); // empty
	}
}
