package ax.xz.max.dns.resource;

import java.lang.foreign.MemorySegment;
import java.util.List;

public record OPTRecord(int payloadSize, byte resultCode, byte version, boolean allowsDnssec, List<Option> options) implements ResourceRecord {
	public OPTRecord {
		options = List.copyOf(options);
	}

	public static final short ID = 41;

	public interface Option { // todo: seal this interface
		short code();
		int dataLength();
		MemorySegment data();

		default int byteSize() {
			return 4 + dataLength();
		}

		default void apply(MemorySegment slice) {
			if (slice.byteSize() < byteSize()) throw new IllegalArgumentException("Slice too small!");

			slice.set(ResourceRecord.NETWORK_SHORT, 0, code());
			slice.set(ResourceRecord.NETWORK_SHORT, 2, (short) dataLength());
			slice.asSlice(4, dataLength()).copyFrom(data());
		}
	}

	public DomainName name() {
		return DomainName.ROOT;
	}

	@Override
	public short classID() {
		return (short) payloadSize;
	}

	@Override
	public int timeToLive() {
		return resultCode << 24
				| version << 16
				| (allowsDnssec ? 1 : 0) << 15;
	}

	@Override
	public void applyData(MemorySegment slice) {
		int offset = 0;

		for (Option option : options) {
			option.apply(slice.asSlice(offset, option.byteSize()));
			offset += option.byteSize();
		}
	}

	@Override
	public int dataLength() {
		int total = 0;
		for (Option option : options) {
			total += option.byteSize();
		}
		return total;
	}

	public static OPTRecord fromData(short classID, int timeToLive, MemorySegment recordData) {
		return new OPTRecord(
				Short.toUnsignedInt(classID),
				(byte) ((timeToLive & 0b1111_1111_0000_0000_0000_0000_0000_0000) >> 24),
				(byte) ((timeToLive & 0b0000_0000_1111_1111_0000_0000_0000_0000) >> 16),
				(timeToLive & 0b0000_0000_0000_0000_1000_0000_0000_0000) != 0,
				List.of() // todo: parse
		);
	}
}
