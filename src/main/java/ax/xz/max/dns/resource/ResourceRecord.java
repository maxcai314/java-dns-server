package ax.xz.max.dns.resource;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;

public sealed interface ResourceRecord permits ARecord, NSRecord {
	DomainName name();

	int timeToLive();

	MemorySegment recordData(SegmentAllocator allocator);
}
