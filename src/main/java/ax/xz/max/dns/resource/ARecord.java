package ax.xz.max.dns.resource;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.net.Inet4Address;
import java.net.UnknownHostException;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.nio.ByteOrder.BIG_ENDIAN;

public record ARecord(DomainName name, int timeToLive, Inet4Address address) implements ResourceRecord {
	public static final short ID = 1;

	@Override
	public void applyData(MemorySegment slice) {
		slice.copyFrom(MemorySegment.ofArray(address.getAddress()));
	}

	@Override
	public int dataLength() {
		return 4;
	}

	private static final ValueLayout.OfByte NETWORK_BYTE = JAVA_BYTE.withByteAlignment(1).withOrder(BIG_ENDIAN);

	public static ARecord fromData(DomainName name, int timeToLive, MemorySegment data) {
		try {
			return new ARecord(name, timeToLive, (Inet4Address) Inet4Address.getByAddress(data.toArray(NETWORK_BYTE)));
		} catch (UnknownHostException e) {
			throw new RuntimeException("Failed to load Ipv4 address", e); // technically impossible
		}
	}
}
