package ax.xz.max.dns.resource;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;

public record NSRecord(DomainName name, int timeToLive, DomainName nameserver) implements ResourceRecord {
	@Override
	public MemorySegment recordData(SegmentAllocator allocator) {
		var segment = allocator.allocate(nameserver.byteSize());
		nameserver.apply(segment);
		return segment;
	}

	public static NSRecord fromData(DomainName name, int timeToLive, MemorySegment data) {
		return new NSRecord(name, timeToLive, DomainName.fromData(data));
	}
}
