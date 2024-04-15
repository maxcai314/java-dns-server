package ax.xz.max.dns.resource;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.net.Inet4Address;
import java.net.UnknownHostException;

public record ARecord(DomainName name, int timeToLive, Inet4Address address) implements ResourceRecord {
	@Override
	public MemorySegment recordData(SegmentAllocator allocator) {
		return MemorySegment.ofArray(address.getAddress());
	}

	public static ARecord fromData(DomainName name, int timeToLive, byte[] data) {
		try {
			return new ARecord(name, timeToLive, (Inet4Address) Inet4Address.getByAddress(data));
		} catch (UnknownHostException e) {
			throw new RuntimeException("Failed to load Ipv4 address", e); // technically impossible
		}
	}
}
