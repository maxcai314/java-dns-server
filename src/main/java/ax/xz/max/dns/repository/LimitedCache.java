package ax.xz.max.dns.repository;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

class LimitedCache<K, V> {
	private final int maxEntries;
	private final ConcurrentHashMap<K, V> data;
	private final ConcurrentLinkedDeque<K> lastAdded = new ConcurrentLinkedDeque<>();

	public LimitedCache(int maxEntries) {
		this.maxEntries = maxEntries;
		this.data = new ConcurrentHashMap<>(maxEntries);
	}

	private void trimOldest() {
		while (data.size() > maxEntries) {
			var key = lastAdded.poll();
			if (key == null) break; // protects from race condition
			data.remove(key);
		}
	}

	public V get(K key) {
		return data.get(key);
	}

	public V put(K key, V value) {
		var result = data.put(key, value);
		if (result == null) { // added something new
			lastAdded.addLast(key);
			trimOldest();
		}
		return result;
	}

	public V computeIfAbsent(K key, MappingFunction<K, V> mappingFunction) throws ResourceAccessException, InterruptedException {
		var result = data.get(key);
		if (result == null) { // couldn't find a match
			result = mappingFunction.valueFor(key);
			put(key, result);
		}
		return result;
	}

	public void clear() {
		lastAdded.clear();
		data.clear(); // shouldn't cause memory leaks
	}

	public String toString() {
		return data.toString();
	}

	public @FunctionalInterface interface MappingFunction<K, V> {
		V valueFor(K key) throws ResourceAccessException, InterruptedException;
	}
}
