package ax.xz.max.dns.resource;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.nio.ByteOrder.BIG_ENDIAN;

public record DomainName(String name) {
	public DomainName {
		name = name.toLowerCase();
		if (name.equals("@")) name = "";
		if (name.startsWith(".")) name = name.substring(1);
		if (!name.endsWith(".")) name += ".";
		if (name.startsWith("-")) throw new IllegalArgumentException("Name cannot start with hyphen: " + name);
		if (name.endsWith("-")) throw new IllegalArgumentException("Name cannot end with hyphen: " + name);
		if (!name.equals(".")) {
			for (String label : name.split("\\.")) {
				if (label.length() > 63) throw new IllegalArgumentException("Label too long: " + label);
				if (!LABEL_TESTER.test(label)) throw new IllegalArgumentException("Invalid label: " + label);
			}
		}
		if (name.length() > 255) throw new IllegalArgumentException("Name too long: " + name);
	}

	private static final Predicate<String> LABEL_TESTER = Pattern.compile("^[a-z0-9-]+$").asMatchPredicate();

	private static final ValueLayout.OfByte NETWORK_BYTE = JAVA_BYTE.withByteAlignment(1).withOrder(BIG_ENDIAN);

	public String[] labels() {
		return name.split("\\.");
	}

	public int byteSize() {
		if (name.equals("."))
			return 1;
		return name.length() + 1; // null-terminated; +2 if name doesn't end with .
	}

	public void apply(MemorySegment slice) {
		if (slice.byteSize() < byteSize()) throw new IllegalArgumentException("Slice too small!");
		long offset = 0;

		for (String label : labels()) {
			slice.set(NETWORK_BYTE, offset, (byte) label.length());
			slice.setString(offset + 1, label, StandardCharsets.US_ASCII);
			offset += 1 + label.length();
		}

		slice.set(NETWORK_BYTE, offset, (byte) 0); // technically unnecessary; null-terminated
	}

	/**
	 * Represents a parsed domain name.
	 * <p>
	 * When parsing a domain name with pointers,
	 * the number of bytes parsed is not necessarily equal to
	 * the length of the data represented.
	 */
	public record ParsedDomainName(DomainName domainName, int bytesParsed) {}

	/**
	 * Does not support compression pointers.
	 */
	public static ParsedDomainName fromData(MemorySegment data) {
		return fromData(data, MemorySegment.NULL);
	}

	/**
	 * Supports compression pointers, referencing the context.
	 * Protects against infinite loops by delegating to a depth-limited helper.
	 */
	public static ParsedDomainName fromData(MemorySegment data, MemorySegment context) {
		return fromData0(data, context, 0);
	}

	/**
	 * Supports compression pointers, referencing the context.
	 * Protects against infinite loops.
	 */
	private static ParsedDomainName fromData0(MemorySegment data, MemorySegment context, int depth) {
		if (depth > 10) throw new IllegalArgumentException("Too many compression pointers- possible infinite loop");

		StringBuilder builder = new StringBuilder();

		int nextIndex = 0; // always points to the next byte to read
		int labelLength = Byte.toUnsignedInt(data.get(NETWORK_BYTE, nextIndex++));
		while (labelLength != 0) {
			if ((labelLength & 0b1100_0000) != 0) {
				// compression pointer
				int pointer = ((labelLength & 0b0011_1111) << 8) | data.get(NETWORK_BYTE, nextIndex++);
				MemorySegment pointerData = context.asSlice(pointer);
				DomainName pointerName = fromData0(pointerData, context, depth + 1).domainName();
				builder.append(pointerName.name);
				return new ParsedDomainName(new DomainName(builder.toString()), nextIndex);
			}

			if (labelLength > 63)
				throw new IllegalArgumentException("Label too long: " + labelLength);

			for (int i=0; i<labelLength; i++) {
				builder.append((char) data.get(NETWORK_BYTE, nextIndex++));
			}

			labelLength = Byte.toUnsignedInt(data.get(NETWORK_BYTE, nextIndex++));
			builder.append('.');
		}

		return new ParsedDomainName(new DomainName(builder.toString()), nextIndex);
	}

	public byte[] bytes() {
		byte[] bytes = new byte[byteSize()];
		MemorySegment segment = MemorySegment.ofArray(bytes);
		apply(segment);
		return bytes;
	}

	public static final DomainName ROOT = new DomainName(".");
}
