package ax.xz.max.dns.resource;

import java.lang.foreign.MemorySegment;

public sealed interface ResourceRecord permits ARecord, NSRecord, CNameRecord {
	DomainName name();

	int timeToLive();

	MemorySegment recordData();
}
