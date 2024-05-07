package ax.xz.max.dns.resource;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import static java.lang.foreign.ValueLayout.JAVA_SHORT;
import static java.nio.ByteOrder.BIG_ENDIAN;

public record DNSHeader(
		short id, boolean isResponse,
		byte opcode, // actually 4 bits
		boolean isAuthoritative,
		boolean isTruncated,
		boolean recursionDesired,
		boolean recursionAvailable,
		byte responseCode,
		short numQuestions,
		short numAnswers,
		short numNS,
		short numAdditional
) {

	private static final ValueLayout.OfShort NETWORK_SHORT = JAVA_SHORT.withByteAlignment(1).withOrder(BIG_ENDIAN);

	public short flags() {
		short result = 0;
		if (isResponse) result |= (short)         0b1000_0000_0000_0000;
		result |= (short) (((int) opcode << 11) & 0b0111_1000_0000_0000);
		if (isAuthoritative) result |= (short)    0b0000_0100_0000_0000;
		if (isTruncated) result |= (short)        0b0000_0010_0000_0000;
		if (recursionDesired) result |= (short)   0b0000_0001_0000_0000;
		if (recursionAvailable) result |= (short) 0b0000_0000_1000_0000;
		result |= (short) (responseCode &         0b0000_0000_0000_1111);

		return result;
	}

	public int byteSize() {
		return 12;
	}

	public void apply(MemorySegment slice) {
		if (slice.byteSize() < byteSize()) throw new IllegalArgumentException("Slice too small!");
		slice.set(NETWORK_SHORT, 0, id);
		slice.set(NETWORK_SHORT, 2, flags());
		slice.set(NETWORK_SHORT, 4, numQuestions);
		slice.set(NETWORK_SHORT, 6, numAnswers);
		slice.set(NETWORK_SHORT, 8, numNS);
		slice.set(NETWORK_SHORT, 10, numAdditional);
	}

	public static DNSHeader fromData(MemorySegment segment) {
		short id = segment.get(NETWORK_SHORT, 0);
		short flags = segment.get(NETWORK_SHORT, 2);
		short numQuestions = segment.get(NETWORK_SHORT, 4);
		short numAnswers = segment.get(NETWORK_SHORT, 6);
		short numNS = segment.get(NETWORK_SHORT, 8);
		short numAdditional = segment.get(NETWORK_SHORT, 10);

		return new DNSHeader(
				id,
				(flags & 0b1000_0000_0000_0000) != 0,
				(byte) ((flags & 0b0111_1000_0000_0000) >> 11),
				(flags & 0b0000_0100_0000_0000) != 0,
				(flags & 0b0000_0010_0000_0000) != 0,
				(flags & 0b0000_0001_0000_0000) != 0,
				(flags & 0b0000_0000_1000_0000) != 0,
				(byte) (flags & 0b0000_0000_0000_1111),
				numQuestions,
				numAnswers,
				numNS,
				numAdditional
		);
	}

	public DNSHeader asMinimalAnswer(short numAnswered, short numAuthority, short numAdditional) {
		return new DNSHeader(
				id,
				true,
				opcode,
				true, // temporarily authoritative
				false,
				false,
				false,
				(byte) 0, // no error
				numQuestions,
				numAnswered, // answer each question
				numAuthority,
				numAdditional
		);
	}

	public DNSHeader asTruncated() {
		return new DNSHeader(
				id,
				isResponse,
				opcode,
				isAuthoritative,
				true,
				recursionDesired,
				recursionAvailable,
				responseCode,
				numQuestions,
				numAnswers,
				numNS,
				numAdditional
		);
	}

	public DNSHeader asErrorResponse() {
		return new DNSHeader(
				id,
				true,
				(byte) 2, // server failure
				isAuthoritative,
				isTruncated,
				recursionDesired,
				recursionAvailable,
				responseCode,
				numQuestions,
				numAnswers,
				numNS,
				numAdditional
		);
	}
}
