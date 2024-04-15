package ax.xz.max.dns.resource;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.nio.ByteOrder.BIG_ENDIAN;

public record DomainName(String name) {
	public DomainName {
		if (name.equals("@")) name = "";
		name = name.toLowerCase();
		if (name.endsWith(".")) name = name.substring(0, name.length() - 1);
		if (name.startsWith(".")) name = name.substring(1);
		if (name.startsWith("-")) throw new IllegalArgumentException("Name cannot start with hyphen: " + name);
		if (name.endsWith("-")) throw new IllegalArgumentException("Name cannot end with hyphen: " + name);
		for (String label : labels()) {
			if (label.length() > 63) throw new IllegalArgumentException("Label too long: " + label);
			if (!label.matches("[a-z0-9-]+")) throw new IllegalArgumentException("Invalid label: " + label);
		}
		if (name.length() > 255) throw new IllegalArgumentException("Name too long: " + name);
	}

	private static final ValueLayout.OfByte NETWORK_BYTE = JAVA_BYTE.withByteAlignment(1).withOrder(BIG_ENDIAN);

	public String[] labels() {
		return name.split("\\.");
	}

	public int byteSize() {
		return name.length() + 1; // null-terminated
	}

	public void apply(MemorySegment slice) {
		if (slice.byteSize() < byteSize()) throw new IllegalArgumentException("Slice too small!");
		long offset = 0;

		for (String label : labels()) {
			slice.set(NETWORK_BYTE, offset, (byte) label.length());
			slice.setString(offset + 1, label, StandardCharsets.US_ASCII);
			offset += 1 + label.length();
		}
	}
}
