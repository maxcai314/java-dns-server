package ax.xz.max.dns.resource;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;

public record NSRecord(DomainName name, int timeToLive) implements ResourceRecord {
	@Override
	public MemorySegment recordData(SegmentAllocator allocator) {
		var segment = allocator.allocate(name.byteSize());
		name.apply(segment);
		return segment;
	}
}
