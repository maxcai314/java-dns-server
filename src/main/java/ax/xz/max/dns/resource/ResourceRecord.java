package ax.xz.max.dns.resource;

import java.lang.foreign.MemorySegment;

public sealed interface ResourceRecord permits ARecord, AAAARecord, CNameRecord, NSRecord {
	DomainName name();

	int timeToLive();

	MemorySegment recordData();

	default short typeID() {
		return switch (this) {
			case ARecord __ -> 1;
			case AAAARecord __ -> 28;
			case NSRecord __ -> 2;
			case CNameRecord __ -> 5;
		};
	}

	static short typeIDOf(Class<? extends ResourceRecord> type) {
		return switch (type.getSimpleName()) {
			case "ARecord" -> 1;
			case "AAAARecord" -> 28;
			case "NSRecord" -> 2;
			case "CNameRecord" -> 5;
			default -> throw new IllegalArgumentException("Unknown class: " + type.getSimpleName());
		};
	}

	static Class<? extends ResourceRecord> fromTypeID(short classID) {
		return switch (classID) {
			case 1 -> ARecord.class;
			case 28 -> AAAARecord.class;
			case 2 -> NSRecord.class;
			case 5 -> CNameRecord.class;
			default -> throw new IllegalArgumentException("Unknown class ID: " + classID);
		};
	}
}
