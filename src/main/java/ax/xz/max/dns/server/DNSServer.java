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

				LinkedList<DNSAnswer> answers = new LinkedList<>();
				LinkedList<DNSAnswer> authorities = new LinkedList<>();
				LinkedList<DNSAnswer> additional = new LinkedList<>();

				for (var query : request.queries()) try {
					var match = repository.getAllByNameAndType(query.name(), query.type()).stream().findAny();
					match.map(DNSAnswer::of).ifPresent(answers::add);

					if (query.type() != CNameRecord.ID && match.isEmpty()) {
						// check if any of the names give CNAME chains
						var cNameMatch = repository.getAllChainsByNameAndType(query.name(), query.type()).stream().findAny();
						if (cNameMatch.isPresent()) {
							answers.add(DNSAnswer.of(cNameMatch.get().record()));
							answers.add(DNSAnswer.of(cNameMatch.get().aliasRecord()));
						}
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
				logger.info("Answers: " + answers);
				logger.info("Authorities: " + authorities);
				logger.info("Additional: " + additional);
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
