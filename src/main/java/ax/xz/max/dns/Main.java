package ax.xz.max.dns;

import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class Main {
	private static final AtomicInteger counter = new AtomicInteger(0);
	public static int printIncrement() {
		int num = counter.incrementAndGet();
		System.out.println(num + " " + Thread.currentThread().getName());
		return num;
	}
	public static void main(String[] args) throws InterruptedException {
		try (var scope = new StructuredTaskScope<>()) {
			System.out.println("Starting...");
			var subtasks = Stream.generate(() -> scope.fork(Main::printIncrement))
					.limit(10)
					.toList();

			scope.join();

			System.out.println("Results: ");
			subtasks.stream()
					.map(StructuredTaskScope.Subtask::get)
					.forEachOrdered(System.out::println);
		}
	}
}
