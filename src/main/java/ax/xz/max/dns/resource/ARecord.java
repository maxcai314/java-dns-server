package ax.xz.max.dns.resource;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.net.Inet4Address;

public record ARecord(DomainName name, int timeToLive, Inet4Address address) implements ResourceRecord {
	@Override
	public MemorySegment recordData(SegmentAllocator allocator) {
		return MemorySegment.ofArray(address.getAddress());
	}
}
