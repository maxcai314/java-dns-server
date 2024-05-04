package ax.xz.max.dns.resource;

import java.lang.foreign.MemorySegment;

public record NSRecord(DomainName name, int timeToLive, DomainName nameserver) implements ResourceRecord {
	@Override
	public MemorySegment recordData() {
		var segment = MemorySegment.ofArray(new byte[nameserver.byteSize()]);
		nameserver.apply(segment);
		return segment;
	}

	public static NSRecord fromData(DomainName name, int timeToLive, MemorySegment data) {
		return new NSRecord(name, timeToLive, DomainName.fromData(data));
	}
}
