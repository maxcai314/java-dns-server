package ax.xz.max.dns.resource;

import java.lang.foreign.MemorySegment;

public record CNameRecord(DomainName name, int timeToLive, DomainName alias) implements ResourceRecord {
	public static final short ID = 5;

	@Override
	public MemorySegment recordData() {
		var segment = MemorySegment.ofArray(new byte[alias.byteSize()]);
		alias.apply(segment);
		return segment;
	}

	public static CNameRecord fromData(DomainName name, int timeToLive, MemorySegment data) {
		return new CNameRecord(name, timeToLive, DomainName.fromData(data));
	}
}
