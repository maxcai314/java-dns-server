package ax.xz.max.dns.resource;

import java.lang.foreign.MemorySegment;

public sealed interface ResourceRecord permits ARecord, AAAARecord, CNameRecord, NSRecord {
	DomainName name();

	int timeToLive();

	MemorySegment recordData();

	default short type() {
		return switch (this) {
			case ARecord __ -> ARecord.ID;
			case AAAARecord __ -> AAAARecord.ID;
			case NSRecord __ -> NSRecord.ID;
			case CNameRecord __ -> CNameRecord.ID;
		};
	}

	static ResourceRecord fromData(DomainName name, short type, int timeToLive, MemorySegment data) {
		return switch (type) {
			case ARecord.ID -> ARecord.fromData(name, timeToLive, data);
			case AAAARecord.ID -> AAAARecord.fromData(name, timeToLive, data);
			case NSRecord.ID -> NSRecord.fromData(name, timeToLive, data);
			case CNameRecord.ID -> CNameRecord.fromData(name, timeToLive, data);
			default -> throw new IllegalArgumentException("Unknown record type: " + type);
		};
	}
}
