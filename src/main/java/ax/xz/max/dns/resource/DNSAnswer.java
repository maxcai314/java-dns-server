package ax.xz.max.dns.resource;

import java.lang.foreign.MemorySegment;

import static java.lang.foreign.ValueLayout.*;
import static java.nio.ByteOrder.BIG_ENDIAN;

/**
 * Represents a DNS resource record, in bytes of memory.
 */
public record DNSAnswer(MemorySegment data) {
	public DNSAnswer {
		data = data.asReadOnly();
	}

	private static final OfByte NETWORK_BYTE = JAVA_BYTE.withByteAlignment(1).withOrder(BIG_ENDIAN);
	private static final OfShort NETWORK_SHORT = JAVA_SHORT.withByteAlignment(1).withOrder(BIG_ENDIAN);
	private static final OfInt NETWORK_INT = JAVA_INT.withByteAlignment(1).withOrder(BIG_ENDIAN);

	public static DNSAnswer of(ResourceRecord resource) {
		int size = (int) resource.recordData().byteSize() + 10 + resource.name().byteSize();
		var segment = MemorySegment.ofArray(new byte[size]);

		var nameSlice = segment.asSlice(0, resource.name().byteSize());
		var trailerSlice = segment.asSlice(nameSlice.byteSize(), 10);
		var rdataSlice = segment.asSlice(nameSlice.byteSize() + 10);

		resource.name().apply(nameSlice);

		trailerSlice.set(NETWORK_SHORT, 0, typeIdOf(resource)); // type
		trailerSlice.set(NETWORK_SHORT, 2, (short) 1); // class
		trailerSlice.set(NETWORK_INT,   4, resource.timeToLive()); // ttl
		trailerSlice.set(NETWORK_SHORT, 8, (short) resource.recordData().byteSize()); // rdlength

		rdataSlice.copyFrom(resource.recordData());
		return new DNSAnswer(segment);
	}

	public int byteSize() {
		return (int) data.byteSize();
	}

	/**
	 * Parses a DNS answer from the start of a memory segment.
	 */
	public static DNSAnswer parseFrom(MemorySegment slice) {
		DomainName name = DomainName.fromData(slice);
		MemorySegment trailer = slice.asSlice(name.byteSize());
		short dataLength = trailer.get(NETWORK_SHORT, name.byteSize() + 8);
		MemorySegment data = slice.asSlice(0, name.byteSize() + 10 + dataLength);
		return new DNSAnswer(data);
	}

	private static short typeIdOf(ResourceRecord resource) {
		return switch(resource) {
			case ARecord __ -> 1;
			case NSRecord __ -> 2;
			case CNameRecord __ -> 5;
		};
	}

	public void apply(MemorySegment slice) {
		if (slice.byteSize() < byteSize()) throw new IllegalArgumentException("Slice too small!");
		slice.copyFrom(data);
	}
}
