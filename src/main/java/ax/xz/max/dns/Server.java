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

			var www = new CNameRecord(
					new DomainName("www.testing.xz.ax"),
					10,
					new DomainName("testing.xz.ax")
			);

			controller.insert(com);
			controller.insert(www);

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
					
					LinkedList<DNSAnswer> answers = new LinkedList<>();
					LinkedList<DNSAnswer> authorities = new LinkedList<>();
					LinkedList<DNSAnswer> additional = new LinkedList<>();
					for (var query : request.queries()) try {
						controller.getAllByType(NSRecord.class).stream()
								.filter(r -> r.nameserver().equals(query.name()))
								.findAny()
								.map(DNSAnswer::of)
								.ifPresent(additional::add);

						controller.getAllByType(CNameRecord.class).stream()
								.filter(r -> r.alias().equals(query.name()))
								.map(DNSAnswer::of)
								.forEach(additional::add);

						switch (query.type().getSimpleName()) {
							case "ARecord", "AAAARecord" -> {
								controller.getAllByType(NSRecord.class).stream()
										.filter(r -> r.name().equals(query.name()))
										.findAny()
										.map(DNSAnswer::of)
										.ifPresent(additional::add);

								controller.getAllByType(CNameRecord.class).stream()
										.filter(r -> r.name().equals(query.name()))
										.map(DNSAnswer::of)
										.forEach(additional::add);
							}

							default ->
								controller.getAllByNameAndType(query.name(), ARecord.class).stream()
										.findAny()
										.map(DNSAnswer::of)
										.ifPresent(authorities::add);
						}

						var match = controller.getAllByNameAndType(query.name(), query.type()).stream()
								.findAny();
						if (match.isEmpty()) continue;
						var answer = match.get();

						answers.add(DNSAnswer.of(answer));

						switch (answer) {
							case NSRecord nsRecord ->
									controller.getAllByNameAndType(nsRecord.nameserver(), ARecord.class).stream()
										.findAny()
										.map(DNSAnswer::of)
										.ifPresent(authorities::add);

							case CNameRecord cNameRecord ->
									controller.getAllByNameAndType(cNameRecord.alias(), ARecord.class).stream()
										.findAny()
										.map(DNSAnswer::of)
										.ifPresent(authorities::add);

							default -> {}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}

					var header = request.header().asMinimalAnswer(
							(short) answers.size(),
							(short) authorities.size(),
							(short) additional.size()
					);
					var response = new DNSMessage(header, request.queries(), answers, authorities, additional);
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
