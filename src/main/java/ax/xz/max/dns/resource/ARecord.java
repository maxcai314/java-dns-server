package ax.xz.max.dns.resource;

public record ARecord(String domain, byte[] ip) {
	public ARecord {
		ip = ip.clone();
	}

	@Override
	public byte[] ip() {
		return ip.clone();
	}
}
