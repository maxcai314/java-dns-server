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
					repository.getAllByType(NSRecord.class).stream()
							.filter(r -> r.nameserver().equals(query.name()))
							.findAny()
							.map(DNSAnswer::of)
							.ifPresent(additional::add);

					repository.getAllByType(CNameRecord.class).stream()
							.filter(r -> r.alias().equals(query.name()))
							.map(DNSAnswer::of)
							.forEach(additional::add);

					if (query.type() == ARecord.class) { // or AAAARecord
						repository.getAllByType(NSRecord.class).stream()
								.filter(r -> r.name().equals(query.name()))
								.findAny()
								.map(DNSAnswer::of)
								.ifPresent(additional::add);

						repository.getAllByType(CNameRecord.class).stream()
								.filter(r -> r.name().equals(query.name()))
								.map(DNSAnswer::of)
								.forEach(additional::add);
					} else {
						repository.getAllByNameAndType(query.name(), ARecord.class).stream()
								.findAny()
								.map(DNSAnswer::of)
								.ifPresent(authorities::add);
					}

					var match = repository.getAllByNameAndType(query.name(), query.type()).stream()
							.findAny();
					if (match.isEmpty()) continue;
					var answer = match.get();

					answers.add(DNSAnswer.of(answer));

					switch (answer) {
						case NSRecord nsRecord ->
								repository.getAllByNameAndType(nsRecord.nameserver(), ARecord.class).stream()
										.findAny()
										.map(DNSAnswer::of)
										.ifPresent(authorities::add);

						case CNameRecord cNameRecord ->
								repository.getAllByNameAndType(cNameRecord.alias(), ARecord.class).stream()
										.findAny()
										.map(DNSAnswer::of)
										.ifPresent(authorities::add);

						default -> {}
					}
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
				logger.info("\nSending response to " + clientAddress);

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
