package ax.xz.max.dns.resource;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.util.ArrayList;
import java.util.List;

public record DNSMessage(DNSHeader header, List<DNSQuery> queries, List<ResourceRecord> answers, List<ResourceRecord> authorities, List<ResourceRecord> additional) {
	public DNSMessage {
		if (header.numQuestions() != queries.size())
			throw new IllegalArgumentException("Number of questions does not match number of queries");
		if (header.numAnswers() != answers.size())
			throw new IllegalArgumentException("Number of answers does not match number of answers");
		if (header.numNS() != authorities.size())
			throw new IllegalArgumentException("Number of NS does not match number of authorities");

		queries = List.copyOf(queries);
		answers = List.copyOf(answers);
		authorities = List.copyOf(authorities);
		additional = List.copyOf(additional);
	}

	/**
	 * Parses the queries of a DNS message.
	 */
	public static DNSMessage parseMessage(MemorySegment data) {
		var header = DNSHeader.fromData(data);
		var querySegment = data.asSlice(header.byteSize());
		int queryOffset = 0;

		ArrayList<DNSQuery> queries = new ArrayList<>(header.numQuestions());

		for (int i = 0; i < header.numQuestions(); i++) {
			var query = DNSQuery.fromData(querySegment.asSlice(queryOffset));
			queryOffset += query.bytesParsed();
			queries.add(query.query());
		}

		// todo: parse these
//		if (!header.isResponse()) {
//			if (header.numAnswers() != 0)
//				throw new IllegalArgumentException("Query cannot contain answers");
//			if (header.numNS() != 0)
//				throw new IllegalArgumentException("Query cannot contain authorities");
//			if (header.numAdditional() != 0)
//				throw new IllegalArgumentException("Query cannot contain additional");
//		}

		return new DNSMessage(header, queries, List.of(), List.of(), List.of());
	}

	public int byteSize() {
		int total = header.byteSize();
		for (var query : queries) total += query.byteSize();
		for (var answer : answers) total += answer.byteSize();
		for (var authority : authorities) total += authority.byteSize();
		for (var additional : additional) total += additional.byteSize();

		return total;
	}

	private boolean needsTruncation(int size) {
		return size > 512;
	}

	public boolean needsTruncation() {
		return needsTruncation(byteSize());
	}

	public MemorySegment toTruncatedMemorySegment(SegmentAllocator allocator) {
		MemorySegment segment = toMemorySegment(allocator);

		// avoid re-calculating bytesize
		var header = needsTruncation((int) segment.byteSize()) ? this.header.asTruncated() : this.header;

		header.apply(segment); // apply the header again

		if (needsTruncation()) return segment.asSlice(0, 512);
		else return segment;
	}

	public MemorySegment toMemorySegment(SegmentAllocator allocator) {
		MemorySegment segment = allocator.allocate(byteSize());
		header.apply(segment);
		int offset = header.byteSize();

		for (var query : queries) {
			query.apply(segment.asSlice(offset));
			offset += query.byteSize();
		}
		for (var answer : answers) {
			answer.apply(segment.asSlice(offset));
			offset += answer.byteSize();
		}
		for (var authority : authorities) {
			authority.apply(segment.asSlice(offset));
			offset += authority.byteSize();
		}
		for (var additional : additional) {
			additional.apply(segment.asSlice(offset));
			offset += additional.byteSize();
		}

		return segment;
	}

	public DNSMessage asErrorResponse() {
		var header = header().asErrorResponse();
		return new DNSMessage(header, queries, List.of(), List.of(), List.of());
	}
}
