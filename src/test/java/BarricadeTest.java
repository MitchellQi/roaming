import org.junit.*;
import static org.junit.Assert.*;
import java.util.*;
import java.util.logging.*;

/**
 * Comprehensive test class for Barricade.java.
 * <p>
 * This test suite aims for 100% code and branch coverage. It covers:
 *   - getWithStateVar: testing both normal and error branches when the internal entrySet changes.
 *   - correctSize: testing normal behavior, entrySet mismatch (throw exception), and size mismatch (log warning).
 *   - putWithStateVar: testing normal insertion/update, entrySet changes, and updated value mismatch.
 *   - correctKeySet & correctEntrySet: testing unmodifiable set behavior and null checks.
 *   - correctStringRepresentation: testing normal matching, entrySet changes (throw exception), and different representation (log warning).
 *   - StateRecoveryOptional: simple instantiation and accessor tests.
 * <p>
 * (Code Coverage, Branch Coverage, Edge Case Coverage)
 */
public class BarricadeTest {

	private final Logger logger = Logger.getLogger(BarricadeTest.class.getName());
	private final LoggerTestingHandler handler = new LoggerTestingHandler();

	@Before
	public void setUp() {
		logger.addHandler(handler);
	}

	@After
	public void tearDown() {
		logger.removeHandler(handler);
		handler.clearLogRecords();
	}

	// ------------------------------------------------------------------
	// 1) getWithStateVar tests
	// ------------------------------------------------------------------

	/**
	 * Test case: getWithStateVar - Normal scenario where the get() result is identical to the snapshot.
	 * Expected: A warning is logged and the original value is returned.
	 * (Code Coverage: normal branch; Branch Coverage: warning branch triggered)
	 */
	@Test
	public void testGetWithStateVar_SameValueTriggersWarning() {
		RoamingMap<Integer, String> map = new RoamingMap<>();
		map.put(10, "ten");
		Barricade.StateRecoveryOptional<String> result = Barricade.getWithStateVar(map, 10);
		assertEquals("ten", result.value());
		Optional<String> log = handler.getLastLog();
		assertTrue("Expected a warning log", log.isPresent());
		assertTrue(log.get().contains("get method of RoamingMap returned incorrect value"));
	}

	/**
	 * Test case: getWithStateVar - Scenario where get() returns a different value than the snapshot.
	 * Expected: No warning is logged and the new value is returned.
	 * (Code Coverage: normal branch; Branch Coverage: non-warning branch)
	 */
	@Test
	public void testGetWithStateVar_DifferentValueNoWarning() {
		RoamingMap<Integer, String> map = new RoamingMap<>();
		map.put(20, "twenty");
		// Override get() to return a different value, but do not change the entrySet.
		RoamingMap<Integer, String> spy = new RoamingMap<>() {
			@Override
			public String get(Object key) {
				return "DIFFERENT";
			}
		};
		spy.putAll(map);
		Barricade.StateRecoveryOptional<String> result = Barricade.getWithStateVar(spy, 20);
		assertEquals("DIFFERENT", result.value());
		assertFalse("No warning log expected", handler.getLastLog().isPresent());
	}

	/**
	 * Test case: getWithStateVar - Misbehavior: entrySet changes during get().
	 * Expected: RuntimeException is thrown because entrySetBefore and entrySetAfter differ.
	 * (Code Coverage: error branch; Branch Coverage: exception branch)
	 */
	@Test(expected = RuntimeException.class)
	public void testGetWithStateVar_EntrySetChangesThrows() {
		// Override get() to modify the map and override entrySet() to return a modified snapshot.
		RoamingMap<Integer, String> map = new RoamingMap<>() {
			private boolean modified = false;
			@Override
			public String get(Object key) {
				if (!modified) {
					modified = true;
					super.put(999, "changedDuringGet");
				}
				return super.get(key);
			}
			@Override
			public Set<Map.Entry<Integer, String>> entrySet() {
				Set<Map.Entry<Integer, String>> base = super.entrySet();
				if (modified) {
					// Return a new set with an extra dummy entry to force a mismatch.
					Set<Map.Entry<Integer, String>> newSet = new HashSet<>(base);
					newSet.add(new SimpleEntry<>(-1, "dummy"));
					return newSet;
				}
				return base;
			}
		};
		map.put(30, "thirty");
		Barricade.getWithStateVar(map, 30);
	}

	/**
	 * Test case: getWithStateVar - Null map argument.
	 * Expected: NullPointerException is thrown.
	 * (Edge Case Coverage)
	 */
	@Test(expected = NullPointerException.class)
	public void testGetWithStateVar_NullMap() {
		Barricade.getWithStateVar(null, 1);
	}

	/**
	 * Test case: getWithStateVar - Null key argument.
	 * Expected: NullPointerException is thrown.
	 * (Edge Case Coverage)
	 */
	@Test(expected = NullPointerException.class)
	public void testGetWithStateVar_NullKey() {
		RoamingMap<Integer, String> map = new RoamingMap<>();
		Barricade.getWithStateVar(map, null);
	}

	// ------------------------------------------------------------------
	// 2) correctSize tests
	// ------------------------------------------------------------------

	/**
	 * Test case: correctSize - Normal scenario where size() is consistent.
	 * Expected: Returns the correct size and no warning is logged.
	 * (Code Coverage: normal branch; Branch Coverage: non-error branch)
	 */
	@Test
	public void testCorrectSize_NormalNoWarning() {
		RoamingMap<Integer, String> map = new RoamingMap<>();
		map.put(1, "one");
		map.put(2, "two");
		int size = Barricade.correctSize(map);
		assertEquals(2, size);
		assertFalse(handler.getLastLog().isPresent());
	}

	/**
	 * Test case: correctSize - EntrySet mismatch scenario.
	 * Override entrySet() so that its second call returns an extra dummy element.
	 * Expected: RuntimeException is thrown.
	 * (Code Coverage: error branch; Branch Coverage: exception branch)
	 */
	@Test(expected = RuntimeException.class)
	public void testCorrectSize_EntrySetMismatchThrows() {
		RoamingMap<Integer, String> map = new RoamingMap<>() {
			private boolean modified = false;
			@Override
			public int size() {
				return super.size();
			}
			@Override
			public Set<Map.Entry<Integer, String>> entrySet() {
				Set<Map.Entry<Integer, String>> base = super.entrySet();
				if (!modified) {
					modified = true;
					return base;
				} else {
					// Return a different set to force an entrySet mismatch.
					Set<Map.Entry<Integer, String>> modSet = new HashSet<>(base);
					modSet.add(new SimpleEntry<>(-1, "dummy"));
					return modSet;
				}
			}
		};
		map.put(1, "one");
		Barricade.correctSize(map);
	}

	/**
	 * Test case: correctSize - Size mismatch logging scenario.
	 * Override size() so that the second call returns super.size() + 1 while entrySet() remains constant.
	 * Expected: A warning is logged and the returned size is the misbehaving size.
	 * (Code Coverage: warning branch; Branch Coverage: size mismatch branch)
	 */
	@Test
	public void testCorrectSize_SizeMismatchLogsWarning() {
		RoamingMap<Integer, String> map = new RoamingMap<>() {
			private boolean firstCall = true;
			@Override
			public int size() {
				if (firstCall) {
					firstCall = false;
					return super.size();
				} else {
					return super.size() + 1;
				}
			}
			@Override
			public Set<Map.Entry<Integer, String>> entrySet() {
				return super.entrySet();
			}
		};
		map.put(1, "one"); // Initially, super.size() == 1.
		int size = Barricade.correctSize(map);
		// Expected: prevSize was 1; the misbehaving size is 2.
		assertEquals(2, size);
		Optional<String> log = handler.getLastLog();
		assertTrue("Expected a warning log", log.isPresent());
		assertTrue(log.get().contains("size method of RoamingMap returned incorrect value"));
	}

	/**
	 * Test case: correctSize - Null map argument.
	 * Expected: NullPointerException is thrown.
	 * (Edge Case Coverage)
	 */
	@Test(expected = NullPointerException.class)
	public void testCorrectSize_NullMap() {
		Barricade.correctSize(null);
	}

	// ------------------------------------------------------------------
	// 3) putWithStateVar tests
	// ------------------------------------------------------------------

	/**
	 * Test case: putWithStateVar - Normal scenario.
	 * Insert a new key and then update it.
	 * Expected: First insertion returns null; update returns the previous value.
	 * (Code Coverage: normal branch; Branch Coverage: update branch)
	 */
	@Test
	public void testPutWithStateVar_Normal() {
		RoamingMap<Integer, String> map = new RoamingMap<>();
		Barricade.StateRecoveryOptional<String> result =
			Barricade.putWithStateVar(map, 10, "TEN");
		assertNull(result.value());
		result = Barricade.putWithStateVar(map, 10, "TEN2");
		assertEquals("TEN", result.value());
	}

	/**
	 * Test case: putWithStateVar - Misbehavior: entrySet changes during put.
	 * Override put() and entrySet() so that entrySet changes after put().
	 * Expected: RuntimeException is thrown.
	 * (Code Coverage: error branch; Branch Coverage: exception branch)
	 */
	@Test(expected = RuntimeException.class)
	public void testPutWithStateVar_EntrySetChanged() {
		RoamingMap<Integer, String> map = new RoamingMap<>() {
			private boolean modified = false;
			@Override
			public String put(Integer key, String value) {
				if (!modified) {
					modified = true;
					super.put(999, "changedDuringPut");
				}
				return super.put(key, value);
			}
			@Override
			public Set<Map.Entry<Integer, String>> entrySet() {
				Set<Map.Entry<Integer, String>> base = super.entrySet();
				if (modified) {
					Set<Map.Entry<Integer, String>> newSet = new HashSet<>(base);
					newSet.add(new SimpleEntry<>(-1, "dummy"));
					return newSet;
				}
				return base;
			}
		};
		Barricade.putWithStateVar(map, 20, "TWENTY");
	}

	/**
	 * Test case: putWithStateVar - Misbehavior: updated value mismatch.
	 * Override get() so that it always returns a value different from the newly inserted value.
	 * Expected: RuntimeException is thrown.
	 * (Code Coverage: error branch; Branch Coverage: exception branch)
	 */
	@Test(expected = RuntimeException.class)
	public void testPutWithStateVar_UpdatedValueMismatch() {
		RoamingMap<Integer, String> map = new RoamingMap<>() {
			@Override
			public String get(Object key) {
				return "NOT_MATCHING";
			}
		};
		map.put(50, "initial");
		Barricade.putWithStateVar(map, 50, "FIFTY");
	}

	/**
	 * Test case: putWithStateVar - Null map argument.
	 * Expected: NullPointerException is thrown.
	 * (Edge Case Coverage)
	 */
	@Test(expected = NullPointerException.class)
	public void testPutWithStateVar_NullMap() {
		Barricade.putWithStateVar(null, 1, "val");
	}

	/**
	 * Test case: putWithStateVar - Null key argument.
	 * Expected: NullPointerException is thrown.
	 * (Edge Case Coverage)
	 */
	@Test(expected = NullPointerException.class)
	public void testPutWithStateVar_NullKey() {
		RoamingMap<Integer, String> map = new RoamingMap<>();
		Barricade.putWithStateVar(map, null, "val");
	}

	/**
	 * Test case: putWithStateVar - Null value argument.
	 * Expected: NullPointerException is thrown.
	 * (Edge Case Coverage)
	 */
	@Test(expected = NullPointerException.class)
	public void testPutWithStateVar_NullValue() {
		RoamingMap<Integer, String> map = new RoamingMap<>();
		Barricade.putWithStateVar(map, 1, null);
	}

	// ------------------------------------------------------------------
	// 4) correctKeySet tests
	// ------------------------------------------------------------------

	/**
	 * Test case: correctKeySet - Normal scenario.
	 * Expected: Returns an unmodifiable set of keys.
	 * (Code Coverage: normal branch; Branch Coverage: unmodifiable check)
	 */
	@Test
	public void testCorrectKeySet_Normal() {
		RoamingMap<Integer, String> map = new RoamingMap<>();
		map.put(1, "one");
		map.put(2, "two");
		Set<Integer> keys = Barricade.correctKeySet(map);
		assertEquals(new HashSet<>(Arrays.asList(1, 2)), keys);
		try {
			keys.add(3);
			fail("Expected UnsupportedOperationException when modifying key set");
		} catch (UnsupportedOperationException e) {
			// Expected behavior.
		}
	}

	/**
	 * Test case: correctKeySet - Null map argument.
	 * Expected: NullPointerException is thrown.
	 * (Edge Case Coverage)
	 */
	@Test(expected = NullPointerException.class)
	public void testCorrectKeySet_NullMap() {
		Barricade.correctKeySet(null);
	}

	// ------------------------------------------------------------------
	// 5) correctEntrySet tests
	// ------------------------------------------------------------------

	/**
	 * Test case: correctEntrySet - Normal scenario.
	 * Expected: Returns an unmodifiable set of entries.
	 * (Code Coverage: normal branch; Branch Coverage: unmodifiable check)
	 */
	@Test
	public void testCorrectEntrySet_Normal() {
		RoamingMap<Integer, String> map = new RoamingMap<>();
		map.put(1, "one");
		map.put(2, "two");
		Set<Map.Entry<Integer, String>> entries = Barricade.correctEntrySet(map);
		assertEquals(2, entries.size());
		try {
			entries.clear();
			fail("Expected UnsupportedOperationException when modifying entry set");
		} catch (UnsupportedOperationException e) {
			// Expected behavior.
		}
	}

	/**
	 * Test case: correctEntrySet - Null map argument.
	 * Expected: NullPointerException is thrown.
	 * (Edge Case Coverage)
	 */
	@Test(expected = NullPointerException.class)
	public void testCorrectEntrySet_NullMap() {
		Barricade.correctEntrySet(null);
	}

	// ------------------------------------------------------------------
	// 6) correctStringRepresentation tests
	// ------------------------------------------------------------------

	/**
	 * Test case: correctStringRepresentation - Normal scenario.
	 * Expected: Returns a string representation matching that of a TreeMap copy,
	 * and no warning is logged.
	 * (Code Coverage: normal branch; Branch Coverage: non-warning branch)
	 */
	@Test
	public void testCorrectStringRepresentation_Normal() {
		RoamingMap<Integer, String> map = new RoamingMap<>();
		map.put(1, "one");
		map.put(2, "two");
		String result = Barricade.correctStringRepresentation(map);
		TreeMap<Integer, String> tm = new TreeMap<>();
		tm.put(1, "one");
		tm.put(2, "two");
		assertEquals(tm.toString(), result);
		assertFalse(handler.getLastLog().isPresent());
	}

	/**
	 * Test case: correctStringRepresentation - EntrySet mismatch scenario.
	 * Override toString() and entrySet() so that entrySet changes between snapshots.
	 * Expected: RuntimeException is thrown.
	 * (Code Coverage: error branch; Branch Coverage: exception branch)
	 */
	@Test(expected = RuntimeException.class)
	public void testCorrectStringRepresentation_EntrySetChanged() {
		RoamingMap<Integer, String> map = new RoamingMap<>() {
			private boolean modified = false;
			@Override
			public String toString() {
				if (!modified) {
					modified = true;
					super.put(999, "changedDuringToString");
				}
				return super.toString();
			}
			@Override
			public Set<Map.Entry<Integer, String>> entrySet() {
				Set<Map.Entry<Integer, String>> base = super.entrySet();
				if (modified) {
					Set<Map.Entry<Integer, String>> newSet = new HashSet<>(base);
					newSet.add(new SimpleEntry<>(-1, "dummy"));
					return newSet;
				}
				return base;
			}
		};
		map.put(1, "one");
		Barricade.correctStringRepresentation(map);
	}

	/**
	 * Test case: correctStringRepresentation - Different representation scenario.
	 * Override toString() to return a different string than the copy,
	 * which should log a warning but not throw an exception.
	 * (Code Coverage: warning branch; Branch Coverage: warning branch)
	 */
	@Test
	public void testCorrectStringRepresentation_DifferentRepresentation() {
		RoamingMap<Integer, String> map = new RoamingMap<>() {
			@Override
			public String toString() {
				return "DIFFERENT";
			}
		};
		map.put(1, "one");
		String str = Barricade.correctStringRepresentation(map);
		assertEquals("DIFFERENT", str);
		Optional<String> log = handler.getLastLog();
		assertTrue("Expected a warning log", log.isPresent());
		assertTrue(log.get().contains("toString method of RoamingMap returned incorrect value"));
	}

	/**
	 * Test case: correctStringRepresentation - Null map argument.
	 * Expected: NullPointerException is thrown.
	 * (Edge Case Coverage)
	 */
	@Test(expected = NullPointerException.class)
	public void testCorrectStringRepresentation_NullMap() {
		Barricade.correctStringRepresentation(null);
	}

	// ------------------------------------------------------------------
	// 7) StateRecoveryOptional tests
	// ------------------------------------------------------------------

	/**
	 * Test case: StateRecoveryOptional - Basic instantiation and accessor test.
	 * Expected: The record returns the correct value and exception.
	 * (Code Coverage: record constructor and accessor methods)
	 */
	@Test
	public void testStateRecoveryOptional() {
		Exception ex = new Exception("testException");
		Barricade.StateRecoveryOptional<String> opt =
			new Barricade.StateRecoveryOptional<>("value", ex);
		assertEquals("value", opt.value());
		assertEquals(ex, opt.exception());
	}
}
