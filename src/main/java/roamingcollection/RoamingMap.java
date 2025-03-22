package roamingcollection;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public final class RoamingMap<K extends Comparable<K>, V> implements NavigableMap<K, V> {

	private final NavigableMap<K, V> internalMap;

	public RoamingMap() {
		this.internalMap = new TreeMap<>();
	}

	public RoamingMap(Map<? extends K, ? extends V> m) {
		this.internalMap = new TreeMap<>();
		putAll(m);
	}

	// Core Map methods

	@Override
	public V put(K key, V value) {
		Objects.requireNonNull(key);
		Objects.requireNonNull(value);
		return internalMap.put(key, value);
	}

	@Override
	public V get(Object key) {
		Objects.requireNonNull(key);
		return internalMap.get(key);
	}

	@Override
	public int size() {
		return internalMap.size();
	}

	@Override
	public Set<K> keySet() {
		return internalMap.keySet();
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		return internalMap.entrySet();
	}

	@Override
	public String toString() {
		return internalMap.toString();
	}

	// Remaining Map interface methods

	@Override
	public V remove(Object key) {
		return internalMap.remove(key);
	}

	@Override
	public void clear() {
		internalMap.clear();
	}

	@Override
	public boolean containsKey(Object key) {
		return internalMap.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return internalMap.containsValue(value);
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		Objects.requireNonNull(m);
		m.forEach(this::put);
	}

	@Override
	public boolean isEmpty() {
		return internalMap.isEmpty();
	}

	@Override
	public Collection<V> values() {
		return internalMap.values();
	}

	// NavigableMap methods

	@Override
	public K lowerKey(K key) {
		return internalMap.lowerKey(key);
	}

	@Override
	public K floorKey(K key) {
		return internalMap.floorKey(key);
	}

	@Override
	public K ceilingKey(K key) {
		return internalMap.ceilingKey(key);
	}

	@Override
	public K higherKey(K key) {
		return internalMap.higherKey(key);
	}

	@Override
	public Entry<K, V> lowerEntry(K key) {
		return internalMap.lowerEntry(key);
	}

	@Override
	public Entry<K, V> floorEntry(K key) {
		return internalMap.floorEntry(key);
	}

	@Override
	public Entry<K, V> ceilingEntry(K key) {
		return internalMap.ceilingEntry(key);
	}

	@Override
	public Entry<K, V> higherEntry(K key) {
		return internalMap.higherEntry(key);
	}

	@Override
	public Entry<K, V> firstEntry() {
		return internalMap.firstEntry();
	}

	@Override
	public Entry<K, V> lastEntry() {
		return internalMap.lastEntry();
	}

	@Override
	public Entry<K, V> pollFirstEntry() {
		return internalMap.pollFirstEntry();
	}

	@Override
	public Entry<K, V> pollLastEntry() {
		return internalMap.pollLastEntry();
	}

	@Override
	public NavigableMap<K, V> descendingMap() {
		return internalMap.descendingMap();
	}

	@Override
	public NavigableSet<K> navigableKeySet() {
		return internalMap.navigableKeySet();
	}

	@Override
	public NavigableSet<K> descendingKeySet() {
		return internalMap.descendingKeySet();
	}

	@Override
	public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
		return internalMap.subMap(fromKey, fromInclusive, toKey, toInclusive);
	}

	@Override
	public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
		return internalMap.headMap(toKey, inclusive);
	}

	@Override
	public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
		return internalMap.tailMap(fromKey, inclusive);
	}

	@Override
	public SortedMap<K, V> subMap(K fromKey, K toKey) {
		return internalMap.subMap(fromKey, true, toKey, false);
	}

	@Override
	public SortedMap<K, V> headMap(K toKey) {
		return internalMap.headMap(toKey, false);
	}

	@Override
	public SortedMap<K, V> tailMap(K fromKey) {
		return internalMap.tailMap(fromKey, true);
	}

	@Override
	public Comparator<? super K> comparator() {
		return internalMap.comparator();
	}

	@Override
	public K firstKey() {
		return internalMap.firstKey();
	}

	@Override
	public K lastKey() {
		return internalMap.lastKey();
	}

}
