package ax.xz.max.dns.resource;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import static ax.xz.max.dns.resource.ResourceRecord.fromTypeID;
import static ax.xz.max.dns.resource.ResourceRecord.typeIDOf;
import static java.lang.foreign.ValueLayout.JAVA_SHORT;
import static java.nio.ByteOrder.BIG_ENDIAN;

/**
 * Represents a DNS query. Each request contains {@link DNSHeader#numQuestions()} queries.
 */
public record DNSQuery(DomainName name, Class<? extends ResourceRecord> type, short classID) {
	public DNSQuery {
		if (type.getSimpleName().equals("ResourceRecord")) throw new IllegalArgumentException("Invalid ResourceRecord");
	}

	private static final ValueLayout.OfShort NETWORK_SHORT = JAVA_SHORT.withByteAlignment(1).withOrder(BIG_ENDIAN);

	public static DNSQuery fromData(MemorySegment data) {
		DomainName name = DomainName.fromData(data);
		var trailer = data.asSlice(name.byteSize(), 4);
		short type = trailer.get(NETWORK_SHORT, 0);
		short classID = trailer.get(NETWORK_SHORT, 2);
		return new DNSQuery(name, fromTypeID(type), classID);
	}

	public int byteSize() {
		return name.byteSize() + 4;
	}

	public void apply(MemorySegment slice) {
		if (slice.byteSize() < byteSize()) throw new IllegalArgumentException("Slice too small!");
		name.apply(slice);
		var trailer = slice.asSlice(name.byteSize(), 4);
		trailer.set(NETWORK_SHORT, 0, typeIDOf(type));
		trailer.set(NETWORK_SHORT, 2, classID);
	}

}
