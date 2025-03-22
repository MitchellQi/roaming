import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

// Can change to cover test cases for bugged program
public final class RoamingMap<K extends Comparable<K>, V> extends TreeMap<K, V> {

	private final Map<K, V> map;

	public RoamingMap() {
		map = new TreeMap<>();
	}

	@Override
	public V get(Object key) {
		Objects.requireNonNull(key);
		return map.get(key);
	}

	@Override
	public int size() {
		return map.size();
	}

	@Override
	public V put(K key, V value) {
		Objects.requireNonNull(key);
		Objects.requireNonNull(value);
		return map.put(key, value);
	}

	@Override
	public Set<K> keySet() {
		return map.keySet();
	}

	@Override
	public Set<Map.Entry<K, V>> entrySet() {
		return map.entrySet();
	}

	public String toString() {
		return map.toString();
	}
}