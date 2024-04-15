package ax.xz.max.dns.resource;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;

public record CNameRecord(DomainName name, int timeToLive, DomainName alias) implements ResourceRecord {
	@Override
	public MemorySegment recordData(SegmentAllocator allocator) {
		var segment = allocator.allocate(alias.byteSize());
		alias.apply(segment);
		return segment;
	}
}