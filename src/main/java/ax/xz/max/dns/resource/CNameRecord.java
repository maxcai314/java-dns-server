package ax.xz.max.dns.resource;

import java.lang.foreign.MemorySegment;

public record CNameRecord(DomainName name, int timeToLive, DomainName alias) implements ResourceRecord {
	public static final short ID = 5;

	@Override
	public void applyData(MemorySegment slice) {
		alias.apply(slice);
	}

	@Override
	public int dataLength() {
		return alias.byteSize();
	}

	public static CNameRecord fromData(DomainName name, int timeToLive, MemorySegment data) {
		return new CNameRecord(name, timeToLive, DomainName.fromData(data).domainName());
	}
}
