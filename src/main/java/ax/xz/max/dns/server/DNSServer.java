package ax.xz.max.dns.server;

import ax.xz.max.dns.repository.ResourceRepository;
import ax.xz.max.dns.resource.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class DNSServer implements AutoCloseable {
	private final Logger logger = LoggerFactory.getLogger(DNSServer.class);
	private final DatagramChannel channel;
	private final ExecutorService executor;
	private final ResourceRepository repository;

	public DNSServer(ResourceRepository repository) throws IOException {
		this(repository, Thread.ofVirtual().factory());
	}

	public DNSServer(ResourceRepository repository, ThreadFactory factory) throws IOException {
		channel = DatagramChannel.open().bind(new InetSocketAddress(53));
		this.repository = repository;
		executor = Executors.newSingleThreadExecutor(factory);
		executor.submit(this::run);
	}

	@Override
	public void close() throws IOException {
		executor.shutdownNow();
		channel.close();
	}
	
	private void run() {
		logger.info("Server started");
		ByteBuffer buffer = ByteBuffer.allocate(65535);

		while (!Thread.interrupted()) {
			try {
				var clientAddress = channel.receive(buffer);
				buffer.flip();
				var segment = MemorySegment.ofBuffer(buffer);

				var request = DNSMessage.parseMessage(segment);

				logger.info("\nReceived request: from " + clientAddress);
				logger.info("Header: " + request.header());
				logger.info("Queries: " + request.queries());
				logger.info("Answers: " + request.answers());
				logger.info("Authorities: " + request.authorities());
				logger.info("Additional: " + request.additional());

				LinkedList<DNSAnswer> answers = new LinkedList<>();
				LinkedList<DNSAnswer> authorities = new LinkedList<>();
				LinkedList<DNSAnswer> additional = new LinkedList<>();

				for (var query : request.queries()) try {
					LinkedList<DomainName> names = new LinkedList<>();
					names.add(query.name());

					if (query.type() != CNameRecord.ID) {
						repository.getAllByNameAndType(query.name(), CNameRecord.ID).stream()
								.map(CNameRecord.class::cast)
								.map(CNameRecord::alias)
								.forEach(names::add); // add other names to try
					}

					// check if any of the names give answers
					var match = names.stream()
							.map(a -> Map.entry(a, repository.getAllByNameAndType(a, query.type()).stream().findAny()))
							.filter(e -> e.getValue().isPresent())
							.map(e -> Map.entry(e.getKey(), e.getValue().get()))
							.findAny();

					if (match.isEmpty()) continue;

					var answerName = match.get().getKey();
					var answer = match.get().getValue();

					if (!answerName.equals(query.name())) // contextualize if necessary
						answers.add(DNSAnswer.of(repository.getAllByNameAndType(query.name(), CNameRecord.ID).getFirst()));
					answers.add(DNSAnswer.of(answer));
				} catch (Exception e) {
					logger.error("Error while processing query", e);
				}

				var header = request.header().asMinimalAnswer(
						(short) answers.size(),
						(short) authorities.size(),
						(short) additional.size()
				);
				var response = new DNSMessage(header, request.queries(), answers, authorities, additional);
				var responseSegment = response.toTruncatedMemorySegment(); // via UDP

				logger.info("Truncating: " + response.needsTruncation());
				logger.info("Response: " + response);
				logger.info("Answers: " + answers);
				logger.info("Sending response to " + clientAddress);

				channel.send(responseSegment.asByteBuffer(), clientAddress);
			} catch (Exception e) {
				logger.error("Error while processing request", e);
			} finally {
				buffer.clear();
			}
		}
		logger.info("Server stopped");
	}
}
