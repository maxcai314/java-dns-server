package ax.xz.max.dns.resource;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.nio.charset.StandardCharsets;

import static java.lang.foreign.ValueLayout.*;
import static java.nio.ByteOrder.BIG_ENDIAN;

public record DNSPacket(MemorySegment data) {
	public DNSPacket {
		data = data.asReadOnly();
	}

	private static final OfByte NETWORK_BYTE = JAVA_BYTE.withByteAlignment(1).withOrder(BIG_ENDIAN);
	private static final OfShort NETWORK_SHORT = JAVA_SHORT.withByteAlignment(1).withOrder(BIG_ENDIAN);
	private static final OfInt NETWORK_INT = JAVA_INT.withByteAlignment(1).withOrder(BIG_ENDIAN);

	public static DNSPacket of(SegmentAllocator allocator, ResourceRecord resource) {
		long size =  resource.recordData(allocator).byteSize() + 10 + resource.name().byteSize();
		var segment = allocator.allocate(size);

		var nameSlice = segment.asSlice(0, resource.name().byteSize());
		var trailerSlice = segment.asSlice(nameSlice.byteSize(), 10);
		var rdataSlice = segment.asSlice(nameSlice.byteSize() + 10);

		resource.name().apply(nameSlice);

		trailerSlice.set(NETWORK_SHORT, 0, typeIdOf(resource)); // type
		trailerSlice.set(NETWORK_SHORT, 2, (short) 1); // class
		trailerSlice.set(NETWORK_INT,   4, resource.timeToLive()); // ttl
		trailerSlice.set(NETWORK_SHORT, 8, (short) resource.recordData(allocator).byteSize()); // rdlength

		rdataSlice.copyFrom(resource.recordData(allocator));
		return new DNSPacket(segment);
	}

	private static short typeIdOf(ResourceRecord resource) {
		return switch(resource) {
			case ARecord __ -> 1;
			case NSRecord __ -> 2;
		};
	}
}
