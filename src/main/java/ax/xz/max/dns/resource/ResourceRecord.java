package ax.xz.max.dns.resource;

public sealed interface ResourceRecord permits ARecord {
	int A_TYPE_ID = 1;
	int AAAA_TYPE_ID = 28;

	 default int typeID() {
		return switch (this) {
			case ARecord __ -> A_TYPE_ID;
			// etc.
		};
	}
}
