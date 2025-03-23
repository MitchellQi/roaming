import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;
import org.junit.Before;
import org.junit.Test;
import roamingcollection.RoamingMap;

public class BarricadeTest {

	// Use provided LoggerTestingHandler
	private LoggerTestingHandler handler;

	@Before
	public void setUp() {
		handler = new LoggerTestingHandler();
		Logger logger = Logger.getLogger(Barricade.class.getName());
		logger.addHandler(handler);
		handler.clearLogRecords();
	}

	// =================== getWithStateVar Tests ===================

	@Test(expected = NullPointerException.class)
	public void testGetWithStateVar_NullMap() {
		Barricade.getWithStateVar(null, "key");
	}

	@Test(expected = NullPointerException.class)
	public void testGetWithStateVar_NullKey() {
		RoamingMap<String, String> map = new RoamingMap<>();
		Barricade.getWithStateVar(map, null);
	}

	@Test
	public void testGetWithStateVar_NormalBranch() {
		RoamingMap<String, String> map = new RoamingMap<>();
		map.put("keyNormal", "valueNormal");
		Barricade.StateRecoveryOptional<String> result = Barricade.getWithStateVar(map, "keyNormal");
		assertEquals("valueNormal", result.value());
		assertFalse("Warning should not be logged", handler.getLastLog().isPresent());
	}

	@Test
	public void testGetWithStateVar_WarningBranch() {
		// Use FaultyGetMap to simulate get() error (returns null) while the copy has the correct value.
		FaultyGetMap faultyMap = new FaultyGetMap();
		faultyMap.put("keyWarning", "valueWarning");
		RoamingMap<String, String> map = new RoamingMap<>(faultyMap, true);
		Barricade.StateRecoveryOptional<String> result = Barricade.getWithStateVar(map, "keyWarning");
		// Although get() returns null, the copy contains "valueWarning".
		assertEquals("valueWarning", result.value());
		Optional<String> log = handler.getLastLog();
		assertTrue("Warning should be logged", log.isPresent());
		assertEquals(
			"get method of RoamingMap returned incorrect value; correct value was used instead",
			log.get());
	}

	@Test(expected = RuntimeException.class)
	public void testGetWithStateVar_EntrySetMismatch() {
		// Use NonMatchingEntrySetMap to simulate mismatch in entrySet() results.
		NonMatchingEntrySetMap mismatchMap = new NonMatchingEntrySetMap();
		mismatchMap.put("keyMismatch", "valueMismatch");
		RoamingMap<String, String> map = new RoamingMap<>(mismatchMap, true);
		Barricade.getWithStateVar(map, "keyMismatch");
	}

	// =================== correctSize Tests ===================

	@Test(expected = NullPointerException.class)
	public void testCorrectSize_NullMap() {
		Barricade.correctSize(null);
	}

	@Test
	public void testCorrectSize_NormalBranch() {
		RoamingMap<String, String> map = new RoamingMap<>();
		map.put("a", "1");
		int size = Barricade.correctSize(map);
		assertEquals(map.size(), size);
		assertFalse("Warning should not be logged", handler.getLastLog().isPresent());
	}

	@Test
	public void testCorrectSize_WarningBranch() {
		FaultySizeMap faultyMap = new FaultySizeMap();
		faultyMap.put("b", "2");
		RoamingMap<String, String> map = new RoamingMap<>(faultyMap, true);
		int size = Barricade.correctSize(map);
		int expectedSize = 2;  // first call returns 1, second returns 2
		assertEquals(expectedSize, size);
		Optional<String> log = handler.getLastLog();
		assertTrue("Warning should be logged", log.isPresent());
		assertEquals(
			"size method of RoamingMap returned incorrect value; correct value was used instead",
			log.get());
	}

	@Test(expected = RuntimeException.class)
	public void testCorrectSize_EntrySetMismatch() {
		NonMatchingEntrySetMap mismatchMap = new NonMatchingEntrySetMap();
		mismatchMap.put("keyMismatchSize", "3");
		RoamingMap<String, String> map = new RoamingMap<>(mismatchMap, true);
		Barricade.correctSize(map);
	}

	// =================== putWithStateVar Tests ===================

	@Test(expected = NullPointerException.class)
	public void testPutWithStateVar_NullMap() {
		Barricade.putWithStateVar(null, "key", "value");
	}

	@Test(expected = NullPointerException.class)
	public void testPutWithStateVar_NullKey() {
		RoamingMap<String, String> map = new RoamingMap<>();
		Barricade.putWithStateVar(map, null, "value");
	}

	@Test(expected = NullPointerException.class)
	public void testPutWithStateVar_NullValue() {
		RoamingMap<String, String> map = new RoamingMap<>();
		Barricade.putWithStateVar(map, "key", null);
	}

	@Test
	public void testPutWithStateVar_NormalBranch() {
		RoamingMap<String, String> map = new RoamingMap<>();
		Barricade.StateRecoveryOptional<String> result = Barricade.putWithStateVar(map, "keyPut",
			"newValue");
		assertNull(result.value());
		assertEquals("newValue", map.get("keyPut"));
		assertFalse("Warning should not be logged", handler.getLastLog().isPresent());
	}

	@Test(expected = RuntimeException.class)
	public void testPutWithStateVar_ValueMismatch() {
		RandomValueMap randomMap = new RandomValueMap();
		randomMap.put("key", "oldValue");
		RoamingMap<String, String> map = new RoamingMap<>(randomMap, true);
		Barricade.putWithStateVar(map, "key", "newValue");
	}

	@Test(expected = RuntimeException.class)
	public void testPutWithStateVar_EntrySetMismatch() {
		ControlledInconsistentMap controlledMap = new ControlledInconsistentMap();
		controlledMap.put("key", "oldValue"); // initial value
		RoamingMap<String, String> map = new RoamingMap<>(controlledMap, true);

		Barricade.putWithStateVar(map, "key", "newValue");
	}

	// =================== correctKeySet Tests ===================

	@Test(expected = NullPointerException.class)
	public void testCorrectKeySet_NullMap() {
		Barricade.correctKeySet(null);
	}

	@Test
	public void testCorrectKeySet_Normal() {
		RoamingMap<String, String> map = new RoamingMap<>();
		map.put("k1", "v1");
		map.put("k2", "v2");
		Set<String> keySet = Barricade.correctKeySet(map);
		assertEquals(map.keySet(), keySet);
		try {
			keySet.add("newKey");
			fail("Expected UnsupportedOperationException");
		} catch (UnsupportedOperationException e) {
			// Expected exception
		}
	}

	// =================== correctEntrySet Tests ===================

	@Test(expected = NullPointerException.class)
	public void testCorrectEntrySet_NullMap() {
		Barricade.correctEntrySet(null);
	}

	@Test
	public void testCorrectEntrySet_Normal() {
		RoamingMap<String, String> map = new RoamingMap<>();
		map.put("k3", "v3");
		Set<Map.Entry<String, String>> entrySet = Barricade.correctEntrySet(map);
		assertEquals(map.entrySet(), entrySet);
		try {
			entrySet.clear();
			fail("Expected UnsupportedOperationException");
		} catch (UnsupportedOperationException e) {
			// Expected exception
		}
	}

	// =================== correctStringRepresentation Tests ===================

	@Test(expected = NullPointerException.class)
	public void testCorrectStringRepresentation_NullMap() {
		Barricade.correctStringRepresentation(null);
	}

	@Test
	public void testCorrectStringRepresentation_NormalBranch() {
		RoamingMap<String, String> map = new RoamingMap<>();
		map.put("a", "b");
		String rep = Barricade.correctStringRepresentation(map);
		String expected = new TreeMap<>(map).toString();
		assertEquals(expected, rep);
		assertFalse("Warning should not be logged", handler.getLastLog().isPresent());
	}

	@Test
	public void testCorrectStringRepresentation_WarningBranch() {
		FaultyToStringMap faultyMap = new FaultyToStringMap();
		faultyMap.put("keyFaulty", "valFaulty");
		RoamingMap<String, String> map = new RoamingMap<>(faultyMap, true);
		String rep = Barricade.correctStringRepresentation(map);
		assertEquals("faulty", rep);
		Optional<String> log = handler.getLastLog();
		assertTrue("Warning should be logged", log.isPresent());
		assertEquals(
			"toString method of RoamingMap returned incorrect value; correct value was used instead",
			log.get());
	}

	@Test(expected = RuntimeException.class)
	public void testCorrectStringRepresentation_EntrySetMismatch() {
		NonMatchingEntrySetMap mismatchMap = new NonMatchingEntrySetMap();
		mismatchMap.put("keyMismatchStr", "valMismatchStr");
		RoamingMap<String, String> map = new RoamingMap<>(mismatchMap, true);
		Barricade.correctStringRepresentation(map);
	}

	// =================== Helper Classes ===================

	// FaultyGetMap: simulate get() error by returning null if value exists.
	static class FaultyGetMap extends TreeMap<String, String> {

		@Override
		public String get(Object key) {
			String value = super.get(key);
			return (value != null) ? null : "faulty";
		}
	}

	// FaultySizeMap: simulate size() error; first call returns normal size, second returns size+1.
	static class FaultySizeMap extends TreeMap<String, String> {

		private int callCount = 0;

		@Override
		public int size() {
			callCount++;
			if (callCount == 1) {
				return super.size();
			} else if (callCount == 2) {
				return super.size() + 1;
			} else {
				return super.size();
			}
		}
	}

	// FaultyToStringMap: simulate toString() error by always returning "faulty".
	static class FaultyToStringMap extends TreeMap<String, String> {

		@Override
		public String toString() {
			return "faulty";
		}
	}

	// NonMatchingEntrySetMap: always returns a different entry set to simulate mismatch.
	static class NonMatchingEntrySetMap extends TreeMap<String, String> {

		private int callCount = 0;

		@Override
		public Set<Map.Entry<String, String>> entrySet() {
			callCount++;
			Set<Map.Entry<String, String>> set = new HashSet<>();
			set.add(new AbstractMap.SimpleEntry<>("dummyKey" + callCount, "dummyValue" + callCount));
			return set;
		}
	}

	// Helper class: RandomValueMap
// When a key-value pair is put in, it stores a random value instead of the provided one.
	static class RandomValueMap extends TreeMap<String, String> {

		@Override
		public String put(String key, String value) {
			// Instead of storing the provided value, store a random value.
			String randomValue = String.valueOf(Math.random());
			return super.put(key, randomValue);
		}
	}

	// ControlledInconsistentMap returns a consistent entry set for the first two calls,
// then returns an inconsistent one (by adding a dummy entry) on later calls.
	static class ControlledInconsistentMap extends TreeMap<String, String> {

		private int entrySetCallCount = 0;
		private Set<Map.Entry<String, String>> consistentSet = null;

		@Override
		public Set<Map.Entry<String, String>> entrySet() {
			entrySetCallCount++;
			if (entrySetCallCount <= 2) {
				if (consistentSet == null) {
					// Cache a copy of the current entry set for consistency.
					consistentSet = new HashSet<>(super.entrySet());
				}
				return consistentSet;
			} else {
				// On the third (or later) call, return a modified set that is different.
				Set<Map.Entry<String, String>> modified = new HashSet<>(super.entrySet());
				modified.add(new AbstractMap.SimpleEntry<>("dummy", "dummy"));
				return modified;
			}
		}
	}


}



