package ax.xz.max.dns.resource;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.util.LinkedList;
import java.util.List;

public record DNSMessage(DNSHeader header, List<DNSQuery> queries, List<DNSAnswer> answers, List<DNSAnswer> authorities, List<DNSAnswer> additional) {
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

	public static DNSMessage parseMessage(MemorySegment data) {
		var header = DNSHeader.fromData(data);
		System.out.println(header);
		var querySegment = data.asSlice(header.byteSize());
		int queryOffset = 0;

		LinkedList<DNSQuery> queries = new LinkedList<>();

		for (int i = 0; i < header.numQuestions(); i++) {
			var query = DNSQuery.fromData(querySegment.asSlice(queryOffset));
			queryOffset += query.byteSize();
			queries.add(query);
		}

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
		return header.byteSize()
				+ queries.stream().mapToInt(DNSQuery::byteSize).sum()
				+ answers.stream().mapToInt(DNSAnswer::byteSize).sum()
				+ authorities.stream().mapToInt(DNSAnswer::byteSize).sum()
				+ additional.stream().mapToInt(DNSAnswer::byteSize).sum();
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
}