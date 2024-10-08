package ax.xz.max.dns.resource;

import java.lang.foreign.MemorySegment;

public record NSRecord(DomainName name, int timeToLive, DomainName nameserver) implements ResourceRecord {
	public static final short ID = 2;

	@Override
	public void applyData(MemorySegment slice) {
		nameserver.apply(slice);
	}

	@Override
	public int dataLength() {
		return nameserver.byteSize();
	}

	public static NSRecord fromData(DomainName name, int timeToLive, MemorySegment data) {
		return new NSRecord(name, timeToLive, DomainName.fromData(data).domainName());
	}
}
