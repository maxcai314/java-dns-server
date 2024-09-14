package ax.xz.max.dns.resource;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import static java.lang.foreign.ValueLayout.*;
import static java.nio.ByteOrder.BIG_ENDIAN;

public sealed interface ResourceRecord permits ARecord, AAAARecord, CNameRecord, NSRecord, OPTRecord {
	DomainName name();

	int timeToLive();

	void applyData(MemorySegment slice);

	int dataLength();

	default short type() {
		return switch (this) {
			case ARecord __ -> ARecord.ID;
			case AAAARecord __ -> AAAARecord.ID;
			case NSRecord __ -> NSRecord.ID;
			case CNameRecord __ -> CNameRecord.ID;
			case OPTRecord __ -> OPTRecord.ID;
		};
	}

	default short classID() {
		return 1; // IN
	}

	static ResourceRecord fromData(DomainName name, short type, short classID, int timeToLive, MemorySegment recordData) {
		return switch (type) {
			case ARecord.ID -> ARecord.fromData(name, timeToLive, recordData);
			case AAAARecord.ID -> AAAARecord.fromData(name, timeToLive, recordData);
			case NSRecord.ID -> NSRecord.fromData(name, timeToLive, recordData);
			case CNameRecord.ID -> CNameRecord.fromData(name, timeToLive, recordData);
			case OPTRecord.ID -> OPTRecord.fromData(classID, timeToLive, recordData);
			default -> throw new IllegalArgumentException("Unknown record type: " + type);
		};
	}

	static ResourceRecord fromData(DomainName name, short type, int timeToLive, MemorySegment recordData) {
		return fromData(name, type, (short) 1, timeToLive, recordData);
	}

	default int byteSize() {
		return name().byteSize() + 10 + dataLength();
	}

	ValueLayout.OfByte NETWORK_BYTE = JAVA_BYTE.withByteAlignment(1).withOrder(BIG_ENDIAN);
	ValueLayout.OfShort NETWORK_SHORT = JAVA_SHORT.withByteAlignment(1).withOrder(BIG_ENDIAN);
	ValueLayout.OfInt NETWORK_INT = JAVA_INT.withByteAlignment(1).withOrder(BIG_ENDIAN);


	default void apply(MemorySegment slice) {
		if (slice.byteSize() < byteSize()) throw new IllegalArgumentException("Slice too small!");

		var nameSlice = slice.asSlice(0, name().byteSize());
		var trailerSlice = slice.asSlice(nameSlice.byteSize(), 10);
		var rdataSlice = slice.asSlice(nameSlice.byteSize() + 10);

		name().apply(nameSlice);

		trailerSlice.set(NETWORK_SHORT, 0, type()); // type
		trailerSlice.set(NETWORK_SHORT, 2, classID()); // class
		trailerSlice.set(NETWORK_INT,   4, timeToLive()); // ttl
		trailerSlice.set(NETWORK_SHORT, 8, (short) dataLength()); // rdlength

		applyData(rdataSlice);
	}

	record ParsedResourceRecord(ResourceRecord record, int bytesParsed) {}

	/**
	 * Parses a DNS answer from the start of a memory segment.
	 */
	static ParsedResourceRecord parseFrom(MemorySegment slice, MemorySegment context) {
		var name = DomainName.fromData(slice, context);
		MemorySegment trailer = slice.asSlice(name.bytesParsed(), 10);
		short type = trailer.get(NETWORK_SHORT, 0);
		short classID = trailer.get(NETWORK_SHORT, 2); // usually 1
		int timeToLive = trailer.get(NETWORK_INT, 4);
		int dataLength = Short.toUnsignedInt(trailer.get(NETWORK_SHORT, 8));
		MemorySegment recordData = slice.asSlice(name.bytesParsed() + 10, dataLength);
		int totalLength = name.bytesParsed() + 10 + dataLength;
		ResourceRecord record = fromData(name.domainName(), type, classID, timeToLive, recordData);
		return new ParsedResourceRecord(record, totalLength);
	}
}
