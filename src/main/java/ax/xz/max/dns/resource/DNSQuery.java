package ax.xz.max.dns.resource;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import static java.lang.foreign.ValueLayout.JAVA_SHORT;
import static java.nio.ByteOrder.BIG_ENDIAN;

/**
 * Represents a DNS query. Each request contains {@link DNSHeader#numQuestions()} queries.
 */
public record DNSQuery(DomainName name, short type, short classID) {
	public record ParsedDNSQuery(DNSQuery query, int bytesParsed) {}
	private static final ValueLayout.OfShort NETWORK_SHORT = JAVA_SHORT.withByteAlignment(1).withOrder(BIG_ENDIAN);

	public static ParsedDNSQuery fromData(MemorySegment data) {
		var name = DomainName.fromData(data);
		var trailer = data.asSlice(name.bytesParsed(), 4);
		short type = trailer.get(NETWORK_SHORT, 0);
		short classID = trailer.get(NETWORK_SHORT, 2);
		return new ParsedDNSQuery(new DNSQuery(name.domainName(), type, classID), name.bytesParsed() + 4);
	}

	public static ParsedDNSQuery fromData(MemorySegment data, MemorySegment context) {
		var name = DomainName.fromData(data, context);
		var trailer = data.asSlice(name.bytesParsed(), 4);
		short type = trailer.get(NETWORK_SHORT, 0);
		short classID = trailer.get(NETWORK_SHORT, 2);
		return new ParsedDNSQuery(new DNSQuery(name.domainName(), type, classID), name.bytesParsed() + 4);
	}

	public int byteSize() {
		return name.byteSize() + 4;
	}

	public void apply(MemorySegment slice) {
		if (slice.byteSize() < byteSize()) throw new IllegalArgumentException("Slice too small!");
		name.apply(slice);
		var trailer = slice.asSlice(name.byteSize(), 4);
		trailer.set(NETWORK_SHORT, 0, type);
		trailer.set(NETWORK_SHORT, 2, classID);
	}

}
