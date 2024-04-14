package ax.xz.max.dns.resource;

import java.util.Arrays;

public record ARecord(String domain, byte[] ip) implements ResourceRecord {
	public ARecord {
		ip = ip.clone();
	}

	@Override
	public byte[] ip() {
		return ip.clone();
	}

	@Override
	public String toString() {
		return "ARecord{" +
				"domain='" + domain + '\'' +
				", ip=" + Arrays.toString(ip) +
				'}';
	}
}
