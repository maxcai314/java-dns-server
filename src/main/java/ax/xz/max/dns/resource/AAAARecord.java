package ax.xz.max.dns.resource;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.net.Inet6Address;
import java.net.UnknownHostException;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.nio.ByteOrder.BIG_ENDIAN;

public record AAAARecord(DomainName name, int timeToLive, Inet6Address address) implements ResourceRecord {
	@Override
	public MemorySegment recordData() {
		return MemorySegment.ofArray(address.getAddress());
	}

	private static final ValueLayout.OfByte NETWORK_BYTE = JAVA_BYTE.withByteAlignment(1).withOrder(BIG_ENDIAN);

	public static AAAARecord fromData(DomainName name, int timeToLive, MemorySegment data) {
		try {
			return new AAAARecord(name, timeToLive, (Inet6Address) Inet6Address.getByAddress(data.toArray(NETWORK_BYTE)));
		} catch (UnknownHostException e) {
			throw new RuntimeException("Failed to load Ipv6 address", e); // technically impossible
		}
	}
}
