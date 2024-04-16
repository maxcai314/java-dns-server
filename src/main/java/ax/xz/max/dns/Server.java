package ax.xz.max.dns;

import ax.xz.max.dns.repository.SQLResourceRepository;
import ax.xz.max.dns.resource.*;

import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.List;
import java.util.LinkedList;

public class Server {
	public static void main(String[] args) throws IOException {
		try (
				var channel = DatagramChannel.open().bind(new InetSocketAddress(53))
		) {
			SQLResourceRepository controller = new SQLResourceRepository();
			controller.clear();

			var com = new ARecord(
					new DomainName("testing.xz.ax"),
					10,
					Inet4Address.ofLiteral("65.108.126.123")
			);

			controller.insert(com);

			ByteBuffer buffer = ByteBuffer.allocate(65535);

			while (!Thread.interrupted()) {
				try {
					var clientAddress = channel.receive(buffer);
					System.out.println(buffer.position() + " bytes received");
					buffer.flip();
					var segment = MemorySegment.ofBuffer(buffer);

					var request = DNSMessage.parseMessage(segment);

					System.out.println("\nReceived request: from " + clientAddress);
					System.out.println("Header: " + request.header());
					System.out.println("Queries: " + request.queries());
					System.out.println("Answers: " + request.answers());
					System.out.println("Authorities: " + request.authorities());
					System.out.println("Additional: " + request.additional());
					System.out.println();
					
					short numAnswered = 0;
					LinkedList<DNSAnswer> answers = new LinkedList<>();
					for (var query : request.queries()) {
						var records = controller.getAllByNameAndType(query.name(), query.clazz());
						if (records.isEmpty()) continue;
						System.out.println("Found records for query");
						answers.add(DNSAnswer.of(records.getFirst()));
						numAnswered++;
					}

					var header = request.header().asMinimalAnswer(numAnswered);
					var response = new DNSMessage(header, request.queries(), answers, List.of(), List.of());
					var responseSegment = response.toTruncatedMemorySegment(); // via UDP

					System.out.println("Truncating: " + response.needsTruncation());
					System.out.println("Response: " + response);
					System.out.println("\nSending response to " + clientAddress);
					channel.send(responseSegment.asByteBuffer(), clientAddress);
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					buffer.clear();
				}
			}

		}
	}
}
