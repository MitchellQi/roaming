import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import roamingcollection.RoamingMap;

// May contain bug(s)
public class Barricade {

	private static final Logger logger = Logger.getLogger(Barricade.class.getName());

	final static <K extends Comparable<K>, V> StateRecoveryOptional<V> getWithStateVar(
		RoamingMap<K, V> roamingMap, K key) {
		Objects.requireNonNull(roamingMap);
		Objects.requireNonNull(key);
		Map<K, V> copy = new TreeMap<>();
		copy.putAll(roamingMap);
		Set<Map.Entry<K, V>> entrySetBefore = copy.entrySet();
		V prevValue = entrySetBefore.stream().filter(entry -> Objects.equals(entry.getKey(), key))
			.map(Map.Entry::getValue).findFirst().orElse(null);
		V value = roamingMap.get(key);
		Set<Map.Entry<K, V>> entrySetAfter = correctEntrySet(roamingMap);
		if (!Objects.equals(entrySetBefore, entrySetAfter)) {
			throw new RuntimeException("get method of RoamingMap operated incorrectly");
		}
		if (Objects.equals(prevValue, value)) {
			logger.log(Level.WARNING,
				"get method of RoamingMap returned incorrect value; correct value was used instead");
			return new StateRecoveryOptional<>(prevValue, null);
		}
		return new StateRecoveryOptional<>(value, null);
	}

	final static <K extends Comparable<K>, V> int correctSize(RoamingMap<K, V> roamingMap) {
		Objects.requireNonNull(roamingMap);
		int prevSize = Collections.unmodifiableNavigableMap(roamingMap).size();
		Set<Map.Entry<K, V>> entrySetBefore = correctEntrySet(roamingMap);
		int size = roamingMap.size();
		Set<Map.Entry<K, V>> entrySetAfter = correctEntrySet(roamingMap);
		if (!Objects.equals(entrySetBefore, entrySetAfter)) {
			throw new RuntimeException("size method of RoamingMap operated incorrectly");
		}
		if (size != prevSize) {
			logger.log(Level.WARNING,
				"size method of RoamingMap returned incorrect value; correct value was used instead");
			return size;
		}
		return prevSize;
	}

	final static <K extends Comparable<K>, V> StateRecoveryOptional<V> putWithStateVar(
		RoamingMap<K, V> roamingMap, K key, V value) {
		Objects.requireNonNull(roamingMap);
		Objects.requireNonNull(key);
		Objects.requireNonNull(value);
		Map<K, V> copy = new TreeMap<>();
		copy.put(key, value);
		Set<Map.Entry<K, V>> prevRoamingSet = copy.entrySet();
		V lastValue = roamingMap.put(key, value);
		V updatedValue = getWithStateVar(roamingMap, key).value();
		Set<Map.Entry<K, V>> newRoamingSet = correctEntrySet(roamingMap);
		if (Objects.equals(updatedValue, value) && Objects.equals(prevRoamingSet, newRoamingSet)) {
			return new StateRecoveryOptional<>(lastValue, null);
		} else {
			throw new RuntimeException("put method of RoamingMap operated incorrectly");
		}
	}

	final static <K extends Comparable<K>, V> Set<K> correctKeySet(RoamingMap<K, V> roamingMap) {
		return Collections.unmodifiableMap(Objects.requireNonNull(roamingMap)).keySet();
	}

	final static <K extends Comparable<K>, V> Set<Map.Entry<K, V>> correctEntrySet(
		RoamingMap<K, V> roamingMap) {
		return Collections.unmodifiableMap(Objects.requireNonNull(roamingMap)).entrySet();
	}

	final static <K extends Comparable<K>, V> String correctStringRepresentation(
		RoamingMap<K, V> roamingMap) {
		Objects.requireNonNull(roamingMap);
		Map<K, V> copy = new TreeMap<>();
		copy.putAll(roamingMap);
		String prevRepresentation = copy.toString();
		Set<Map.Entry<K, V>> roamingSetBefore = correctEntrySet(roamingMap);
		String representation = roamingMap.toString();
		Set<Map.Entry<K, V>> roamingSetAfter = correctEntrySet(roamingMap);
		if (!Objects.equals(roamingSetBefore, roamingSetAfter)) {
			throw new RuntimeException("toString method of RoamingMap operated incorrectly");
		}
		if (!Objects.equals(prevRepresentation, representation)) {
			logger.log(Level.WARNING,
				"toString method of RoamingMap returned incorrect value; correct value was used instead");
		}
		return representation;
	}

	record StateRecoveryOptional<V>(V value, Exception exception) {

	}
}
