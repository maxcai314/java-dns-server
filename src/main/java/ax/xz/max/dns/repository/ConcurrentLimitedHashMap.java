package ax.xz.max.dns.repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

class ConcurrentLimitedHashMap<K, V> implements Map<K, V> {
	private final int maxEntries;
	private final ConcurrentHashMap<K, V> data;
	private final ConcurrentLinkedDeque<K> lastAccessed = new ConcurrentLinkedDeque<>();

	public ConcurrentLimitedHashMap(int maxEntries) {
		this.maxEntries = maxEntries;
		this.data = new ConcurrentHashMap<>(maxEntries);
	}

	private void trimOldest() {
		while (data.size() > maxEntries) {
			data.remove(lastAccessed.pop());
		}
	}

	public int size() {
		return data.size();
	}
	public boolean isEmpty() {
		return data.isEmpty();
	}
	public boolean containsKey(Object key) {
		return data.containsKey(key);
	}
	public boolean containsValue(Object value) {
		return data.containsValue(value);
	}
	public V get(Object key) {
		return data.get(key);
	}

	public V put(K key, V value) {
		var result = data.put(key, value);
		lastAccessed.addLast(key);
		trimOldest();
		return result;
	}
	public V remove(Object key) {
		return data.remove(key);
	}
	public void putAll(Map<? extends K, ? extends V> map) {
		data.putAll(map);
		map.keySet().forEach(lastAccessed::addLast);
		trimOldest();
	}
	public void clear() {
		data.clear();
		lastAccessed.clear();
	}

	public Set<K> keySet() {
		return data.keySet();
	}

	public Set<Map.Entry<K, V>> entrySet() {
		return data.entrySet();
	}

	public Collection<V> values() {
		return data.values();
	}

	public boolean equals(Object o) {
		return data.equals(o);
	}
	public int hashCode() {
		return data.hashCode();
	}
	public String toString() {
		return data.toString();
	}

	// Override default methods in Map
	@Override
	public V getOrDefault(Object k, V defaultValue) {
		return data.getOrDefault(k, defaultValue);
	}
	@Override
	public void forEach(BiConsumer<? super K, ? super V> action) {
		data.forEach(action);
	}
	@Override
	public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
		data.replaceAll(function);
	}
	@Override
	public V putIfAbsent(K key, V value) {
		return data.putIfAbsent(key, value);
	}
	@Override
	public boolean remove(Object key, Object value) {
		return data.remove(key, value);
	}
	@Override
	public boolean replace(K key, V oldValue, V newValue) {
		return data.replace(key, oldValue, newValue);
	}
	@Override
	public V replace(K key, V value) {
		return data.replace(key, value);
	}
	@Override
	public V computeIfAbsent(K key,
	                         Function<? super K, ? extends V> mappingFunction) {
		boolean added = !data.containsKey(key);
		var result = data.computeIfAbsent(key, mappingFunction);
		if (added) {
			lastAccessed.addLast(key);
			trimOldest();
		}
		return result;
	}
	@Override
	public V computeIfPresent(K key,
	                          BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
		return data.computeIfPresent(key, remappingFunction);
	}
	@Override
	public V compute(K key,
	                 BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
		return data.compute(key, remappingFunction);
	}
	@Override
	public V merge(K key, V value,
	               BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
		return data.merge(key, value, remappingFunction);
	}
}
