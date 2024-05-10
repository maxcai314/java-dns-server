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
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;

public class DNSServer implements AutoCloseable {
	private final Logger logger;
	private final DatagramChannel datagramChannel;
	private final ServerSocketChannel serverSocketChannel;
	private final ExecutorService executor;
	private final ResourceRepository repository;

	public DNSServer(ResourceRepository repository) throws IOException {
		this(repository, Thread.ofVirtual().factory());
	}

	public DNSServer(ResourceRepository repository, ThreadFactory threadFactory) throws IOException {
		this.repository = repository;
		this.logger = LoggerFactory.getLogger(DNSServer.class);

		this.datagramChannel = DatagramChannel.open().bind(new InetSocketAddress(53));
		this.serverSocketChannel = ServerSocketChannel.open().bind(new InetSocketAddress(53));
		this.executor = Executors.newThreadPerTaskExecutor(threadFactory);

		executor.submit(this::runDatagramServer);
		executor.submit(this::runSocketServer);
	}

	@Override
	public void close() throws IOException {
		executor.shutdownNow();
		datagramChannel.close();
		serverSocketChannel.close();
	}

	private DNSMessage responseFor(DNSMessage request) {
		Instant start = Instant.now();

		ArrayList<ResourceRecord> answers = new ArrayList<>();
		ArrayList<ResourceRecord> authorities = new ArrayList<>();
		ArrayList<ResourceRecord> additional = new ArrayList<>();

		for (var query : request.queries())
			try {
				var match = repository.getAllByNameAndType(query.name(), query.type());
				answers.addAll(match);

				if (query.type() != CNameRecord.ID && match.isEmpty()) {
					// check if any of the names give CNAME chains
					var cNameMatch = repository.getAllChainsByNameAndType(query.name(), query.type());
					for (var chain : cNameMatch) {
						answers.add(chain.record());
						answers.add(chain.aliasRecord());
					}
				}
			} catch (Exception e) {
				logger.error("Error while processing query", e);
				logger.info("Returning a server failure response");
				return request.asErrorResponse();
			}

		var header = request.header().asMinimalAnswer(
				(short) answers.size(),
				(short) authorities.size(),
				(short) additional.size()
		);

		logger.info("Search took " + Duration.between(start, Instant.now()));

		return new DNSMessage(header, request.queries(), answers, authorities, additional);
	}
	
	private void runDatagramServer() {
		logger.info("UDP Server started");
		ByteBuffer buffer = ByteBuffer.allocate(65535);

		while (!Thread.interrupted()) {
			try {
				var clientAddress = datagramChannel.receive(buffer);
				Instant start = Instant.now();
				buffer.flip();
				var segment = MemorySegment.ofBuffer(buffer);

				var request = DNSMessage.parseMessage(segment);

				logger.info("Received UDP request from " + clientAddress);
				logger.info("Header: " + request.header());
				logger.info("Queries: " + request.queries());

				var response = responseFor(request);
				var responseSegment = response.toTruncatedMemorySegment(); // via UDP

				logger.info("Truncating: " + response.needsTruncation());
				logger.info("Response: " + response);
				logger.info("Answers: " + response.answers());
				logger.info("Authorities: " + response.authorities());
				logger.info("Additional: " + response.additional());
				logger.info("Sending response to " + clientAddress);

				datagramChannel.send(responseSegment.asByteBuffer(), clientAddress);
				logger.info("UDP Response took " + Duration.between(start, Instant.now()));
			} catch (Exception e) {
				logger.error("Error while processing request", e);
			} finally {
				buffer.clear();
			}
		}
		logger.info("Server stopped");
	}

	private void runSocketServer() {
		logger.info("TCP Server Socket Channel");

		while (!Thread.interrupted()) {
			try {
				var clientChannel = serverSocketChannel.accept();
				logger.info("TCP connection accepted from " + clientChannel.getRemoteAddress());
				executor.submit(() -> handleSocketConnection(clientChannel));
			} catch (Exception e) {
				logger.error("Error while accepting TCP connection", e);
			}
		}
		logger.info("TCP Server Socket Channel stopped");
	}

	private void handleSocketConnection(SocketChannel clientChannel) {
		try (clientChannel) {
			while (!Thread.interrupted()) {
				Instant start = Instant.now();
				ByteBuffer lengthBuffer = ByteBuffer.allocate(2);

				while (lengthBuffer.hasRemaining())
					clientChannel.read(lengthBuffer); // read 2 bytes for message length
				lengthBuffer.flip();
				int length = lengthBuffer.getShort();

				logger.info("TCP Message length: " + length + " bytes");

				ByteBuffer buffer = ByteBuffer.allocate(length);

				while (buffer.hasRemaining())
					clientChannel.read(buffer);
				buffer.flip();
				var segment = MemorySegment.ofBuffer(buffer);

				var request = DNSMessage.parseMessage(segment);

				logger.info("Received TCP request from " + clientChannel.getRemoteAddress());
				logger.info("Header: " + request.header());
				logger.info("Queries: " + request.queries());

				var response = responseFor(request);
				var responseSegment = response.toMemorySegment(); // via TCP

				logger.info("Response: " + response);
				logger.info("Answers: " + response.answers());
				logger.info("Authorities: " + response.authorities());
				logger.info("Additional: " + response.additional());
				logger.info("Sending response to " + clientChannel.getRemoteAddress());

				clientChannel.write(responseSegment.asByteBuffer());
				logger.info("TCP Response took " + Duration.between(start, Instant.now()));
			}
		} catch (Exception e) {
			logger.error("Error while processing request", e);
		}
	}
}
