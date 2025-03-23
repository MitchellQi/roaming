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

	// Test logger handler for capturing log messages
	private TestLoggerHandler logHandler;

	@Before
	public void setUp() {
		logHandler = new TestLoggerHandler();
		Logger logger = Logger.getLogger(Barricade.class.getName());
		logger.addHandler(logHandler);
		logHandler.clearLogRecords();
	}

	// ==================== getWithStateVar Tests ====================

	// Edge Case: Null map should throw NullPointerException.
	@Test(expected = NullPointerException.class)
	public void testGetWithStateVar_NullMap() {
		Barricade.getWithStateVar(null, "key");
	}

	// Edge Case: Null key should throw NullPointerException.
	@Test(expected = NullPointerException.class)
	public void testGetWithStateVar_NullKey() {
		RoamingMap<String, String> map = new RoamingMap<>();
		Barricade.getWithStateVar(map, null);
	}

	// Code Coverage: Normal branch of getWithStateVar without mismatches.
	@Test
	public void testGetWithStateVar_Normal() {
		RoamingMap<String, String> map = new RoamingMap<>();
		map.put("keyNormal", "valueNormal");
		Barricade.StateRecoveryOptional<String> result = Barricade.getWithStateVar(map, "keyNormal");
		assertEquals("valueNormal", result.value());
		assertFalse("No warning should be logged", logHandler.getLastLog().isPresent());
	}

	// Mismatch: get() returns incorrect value, so fallback to copy value.
	@Test
	public void testGetWithStateVar_ValueMismatch() {
		FaultyGetMap faultyMap = new FaultyGetMap();
		faultyMap.put("keyWarning", "valueWarning");
		RoamingMap<String, String> map = new RoamingMap<>(faultyMap, true);
		Barricade.StateRecoveryOptional<String> result = Barricade.getWithStateVar(map, "keyWarning");
		// Although get() returns null, the copy contains "valueWarning".
		assertEquals("valueWarning", result.value());
		Optional<String> log = logHandler.getLastLog();
		assertTrue("Warning should be logged", log.isPresent());
		assertEquals(
			"get method of RoamingMap returned incorrect value; correct value was used instead",
			log.get());
	}

	// Edge Case / Mismatch: EntrySet mismatch should throw RuntimeException.
	@Test(expected = RuntimeException.class)
	public void testGetWithStateVar_EntrySetMismatch() {
		MismatchEntrySetMap mismatchMap = new MismatchEntrySetMap();
		mismatchMap.put("keyMismatch", "valueMismatch");
		RoamingMap<String, String> map = new RoamingMap<>(mismatchMap, true);
		Barricade.getWithStateVar(map, "keyMismatch");
	}

	// ==================== correctSize Tests ====================

	// Edge Case: Null map should throw NullPointerException.
	@Test(expected = NullPointerException.class)
	public void testCorrectSize_NullMap() {
		Barricade.correctSize(null);
	}

	// Code Coverage: Normal branch of correctSize.
	@Test
	public void testCorrectSize_Normal() {
		RoamingMap<String, String> map = new RoamingMap<>();
		map.put("a", "1");
		int size = Barricade.correctSize(map);
		assertEquals(map.size(), size);
		assertFalse("No warning should be logged", logHandler.getLastLog().isPresent());
	}

	// Mismatch: size() returns inconsistent value, so correctSize returns the mismatched size.
	@Test
	public void testCorrectSize_SizeMismatch() {
		FaultySizeMap faultyMap = new FaultySizeMap();
		faultyMap.put("b", "2");
		RoamingMap<String, String> map = new RoamingMap<>(faultyMap, true);
		int size = Barricade.correctSize(map);
		int expectedSize = 2;  // First call returns normal size, second call returns size+1.
		assertEquals(expectedSize, size);
		Optional<String> log = logHandler.getLastLog();
		assertTrue("Warning should be logged", log.isPresent());
		assertEquals(
			"size method of RoamingMap returned incorrect value; correct value was used instead",
			log.get());
	}

	// Edge Case / Mismatch: EntrySet mismatch should throw RuntimeException.
	@Test(expected = RuntimeException.class)
	public void testCorrectSize_EntrySetMismatch() {
		MismatchEntrySetMap mismatchMap = new MismatchEntrySetMap();
		mismatchMap.put("keyMismatchSize", "3");
		RoamingMap<String, String> map = new RoamingMap<>(mismatchMap, true);
		Barricade.correctSize(map);
	}

	// ==================== putWithStateVar Tests ====================

	// Edge Case: Null map should throw NullPointerException.
	@Test(expected = NullPointerException.class)
	public void testPutWithStateVar_NullMap() {
		Barricade.putWithStateVar(null, "key", "value");
	}

	// Edge Case: Null key should throw NullPointerException.
	@Test(expected = NullPointerException.class)
	public void testPutWithStateVar_NullKey() {
		RoamingMap<String, String> map = new RoamingMap<>();
		Barricade.putWithStateVar(map, null, "value");
	}

	// Edge Case: Null value should throw NullPointerException.
	@Test(expected = NullPointerException.class)
	public void testPutWithStateVar_NullValue() {
		RoamingMap<String, String> map = new RoamingMap<>();
		Barricade.putWithStateVar(map, "key", null);
	}

	// Code Coverage: Normal branch of putWithStateVar.
	@Test
	public void testPutWithStateVar_Normal() {
		RoamingMap<String, String> map = new RoamingMap<>();
		Barricade.StateRecoveryOptional<String> result = Barricade.putWithStateVar(map, "keyPut",
			"newValue");
		assertNull(result.value());
		assertEquals("newValue", map.get("keyPut"));
		assertFalse("No warning should be logged", logHandler.getLastLog().isPresent());
	}

	// Mismatch: Inconsistent value update should throw RuntimeException.
	@Test(expected = RuntimeException.class)
	public void testPutWithStateVar_ValueMismatch() {
		RandomValueMap randomMap = new RandomValueMap();
		randomMap.put("key", "oldValue");
		RoamingMap<String, String> map = new RoamingMap<>(randomMap, true);
		Barricade.putWithStateVar(map, "key", "newValue");
	}

	// Edge Case / Mismatch: EntrySet mismatch should throw RuntimeException.
	@Test(expected = RuntimeException.class)
	public void testPutWithStateVar_EntrySetMismatch() {
		ControlledMismatchMap controlledMap = new ControlledMismatchMap();
		controlledMap.put("key", "oldValue"); // initial value
		RoamingMap<String, String> map = new RoamingMap<>(controlledMap, true);
		Barricade.putWithStateVar(map, "key", "newValue");
	}

	// ==================== correctKeySet Tests ====================

	// Edge Case: Null map should throw NullPointerException.
	@Test(expected = NullPointerException.class)
	public void testCorrectKeySet_NullMap() {
		Barricade.correctKeySet(null);
	}

	// Code Coverage: Normal branch of correctKeySet.
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
			// Expected exception.
		}
	}

	// ==================== correctEntrySet Tests ====================

	// Edge Case: Null map should throw NullPointerException.
	@Test(expected = NullPointerException.class)
	public void testCorrectEntrySet_NullMap() {
		Barricade.correctEntrySet(null);
	}

	// Code Coverage: Normal branch of correctEntrySet.
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
			// Expected exception.
		}
	}

	// ==================== correctStringRepresentation Tests ====================

	// Edge Case: Null map should throw NullPointerException.
	@Test(expected = NullPointerException.class)
	public void testCorrectStringRepresentation_NullMap() {
		Barricade.correctStringRepresentation(null);
	}

	// Code Coverage: Normal branch of correctStringRepresentation.
	@Test
	public void testCorrectStringRepresentation_Normal() {
		RoamingMap<String, String> map = new RoamingMap<>();
		map.put("a", "b");
		String rep = Barricade.correctStringRepresentation(map);
		String expected = new TreeMap<>(map).toString();
		assertEquals(expected, rep);
		assertFalse("No warning should be logged", logHandler.getLastLog().isPresent());
	}

	// Mismatch: Incorrect toString() result triggers warning.
	@Test
	public void testCorrectStringRepresentation_StringMismatch() {
		FaultyToStringMap faultyMap = new FaultyToStringMap();
		faultyMap.put("keyFaulty", "valFaulty");
		RoamingMap<String, String> map = new RoamingMap<>(faultyMap, true);
		String rep = Barricade.correctStringRepresentation(map);
		assertEquals("faulty", rep);
		Optional<String> log = logHandler.getLastLog();
		assertTrue("Warning should be logged", log.isPresent());
		assertEquals(
			"toString method of RoamingMap returned incorrect value; correct value was used instead",
			log.get());
	}

	// Edge Case / Mismatch: EntrySet mismatch in toString should throw RuntimeException.
	@Test(expected = RuntimeException.class)
	public void testCorrectStringRepresentation_EntrySetMismatch() {
		MismatchEntrySetMap mismatchMap = new MismatchEntrySetMap();
		mismatchMap.put("keyMismatchStr", "valMismatchStr");
		RoamingMap<String, String> map = new RoamingMap<>(mismatchMap, true);
		Barricade.correctStringRepresentation(map);
	}

	// ==================== Helper Classes ====================

	// Logger handler for capturing log output in tests.
	static class TestLoggerHandler extends java.util.logging.Handler {

		private final java.util.List<String> logs = new java.util.ArrayList<>();

		@Override
		public void publish(java.util.logging.LogRecord record) {
			logs.add(record.getMessage());
		}

		@Override
		public void flush() {
		}

		@Override
		public void close() throws SecurityException {
		}

		public Optional<String> getLastLog() {
			if (logs.isEmpty()) {
				return Optional.empty();
			}
			return Optional.of(logs.get(logs.size() - 1));
		}

		public void clearLogRecords() {
			logs.clear();
		}
	}

	// Helper class: FaultyGetMap simulates a get() error by returning null if the value exists.
	static class FaultyGetMap extends TreeMap<String, String> {

		@Override
		public String get(Object key) {
			String value = super.get(key);
			return (value != null) ? null : "faulty";
		}
	}

	// Helper class: FaultySizeMap simulates a size() error; first call returns normal size, second returns size+1.
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

	// Helper class: FaultyToStringMap simulates a toString() error by always returning "faulty".
	static class FaultyToStringMap extends TreeMap<String, String> {

		@Override
		public String toString() {
			return "faulty";
		}
	}

	// Helper class: MismatchEntrySetMap always returns a different entry set to simulate a mismatch.
	static class MismatchEntrySetMap extends TreeMap<String, String> {

		private int callCount = 0;

		@Override
		public Set<Map.Entry<String, String>> entrySet() {
			callCount++;
			Set<Map.Entry<String, String>> set = new HashSet<>();
			set.add(new AbstractMap.SimpleEntry<>("dummyKey" + callCount, "dummyValue" + callCount));
			return set;
		}
	}

	// Helper class: RandomValueMap stores a random value instead of the provided one.
	static class RandomValueMap extends TreeMap<String, String> {

		@Override
		public String put(String key, String value) {
			String randomValue = String.valueOf(Math.random());
			return super.put(key, randomValue);
		}
	}

	// Helper class: ControlledMismatchMap returns a consistent entry set for the first two calls,
	// then returns an inconsistent one (by adding a dummy entry) on later calls.
	static class ControlledMismatchMap extends TreeMap<String, String> {

		private int entrySetCallCount = 0;
		private Set<Map.Entry<String, String>> consistentSet = null;

		@Override
		public Set<Map.Entry<String, String>> entrySet() {
			entrySetCallCount++;
			if (entrySetCallCount <= 2) {
				if (consistentSet == null) {
					consistentSet = new HashSet<>(super.entrySet());
				}
				return consistentSet;
			} else {
				Set<Map.Entry<String, String>> modified = new HashSet<>(super.entrySet());
				modified.add(new AbstractMap.SimpleEntry<>("dummy", "dummy"));
				return modified;
			}
		}
	}
}
