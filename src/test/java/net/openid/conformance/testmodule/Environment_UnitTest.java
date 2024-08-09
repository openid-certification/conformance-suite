package net.openid.conformance.testmodule;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class Environment_UnitTest {

	private Environment env;


	private JsonObject testObject;

	private JsonObject altObject;

	private String testKey;

	private String notFoundKey;

	private String altKey;

	private String mappedKey;

	private String path;

	private String intPath;

	private String longPath;

	private String pathNotFound;

	private String testStringKey;

	private String testStringValue;

	private String altStringValue;

	private JsonArray subArray;

	private JsonObject subObject;


	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	public void setUp() throws Exception {

		env = new Environment();

		testKey = "test";

		notFoundKey = "not_found";

		altKey = "alt";

		mappedKey = "mapped";

		testObject = JsonParser.parseString("""
				{
					"long": 123465478745287987,
					"string": "value",
					"stringint": "3",
					"stringbool": "true",
					"array": [1, 2, "a", "b"],
					"object": {
						"int": 1234,
						"foo": "bar",
						"baz": {
							"qux": "batman"
						}
					}
				}""").getAsJsonObject();

		subArray = JsonParser.parseString("[1, 2, \"a\", \"b\"]").getAsJsonArray();

		subObject = JsonParser.parseString("""
				{
						"int": 1234,
						"foo": "bar",
						"baz": {
							"qux": "batman"
						}
					}
				""").getAsJsonObject();

		path = "object.baz.qux";

		longPath = "long";

		intPath = "object.int";

		pathNotFound = "apple.banana.republic";

		altObject = JsonParser.parseString("""
				{
					"number": 9876,
					"thing": "evaluation",
					"list": [10, 20, "z", "w"],
					"box": {
						"bat": "man",
						"man": {
							"batman": "yes"
						}
					}
				}""").getAsJsonObject();


		testStringKey = "test_string";

		testStringValue = "Test String Value";

		altStringValue = "Alternative String Value";

		env.putObject(testKey, testObject);
		env.putString(testStringKey, testStringValue);

	}

	/**
	 * Test method for {@link Environment#containsObject(java.lang.String)}.
	 */
	@Test
	public void testContainsObj() {

		assertTrue(env.containsObject(testKey));
		assertFalse(env.containsObject(notFoundKey));

	}

	/**
	 * Test method for {@link Environment#getObject(java.lang.String)}.
	 */
	@Test
	public void testGet() {

		JsonObject testGet = env.getObject(testKey);

		assertEquals(testObject, testGet);

		JsonObject notFoundGet = env.getObject(notFoundKey);

		assertNull(notFoundGet);
	}

	/**
	 * Test method for {@link Environment#removeObject(java.lang.String)}.
	 */
	@Test
	public void testRemove() {

		// add a secondary object
		env.putObject(altKey, altObject);

		env.removeObject(testKey);

		assertFalse(env.containsObject(testKey));
		assertNull(env.getObject(testKey));

		// make sure other object is not altered
		assertEquals(altObject, env.getObject(altKey));

	}

	/**
	 * Test method for {@link Environment#getString(java.lang.String)}.
	 */
	@Test
	public void testGetStringString() {

		String stringGet = env.getString(testStringKey);

		assertEquals(testStringValue, stringGet);

		String notFoundGet = env.getString(notFoundKey);

		assertNull(notFoundGet);

	}

	/**
	 * Test method for {@link Environment#putObject(java.lang.String, com.google.gson.JsonObject)}.
	 */
	@Test
	public void testPut() {

		assertFalse(env.containsObject(altKey));
		assertNull(env.getObject(altKey));

		env.putObject(altKey, altObject);

		assertTrue(env.containsObject(altKey));
		assertEquals(altObject, env.getObject(altKey));

	}

	@Test
	public void testPutWithPathSingle() {

		assertFalse(env.containsObject(altKey));
		assertNull(env.getObject(altKey));

		env.putObject(altKey, "foo", altObject);

		assertTrue(env.containsObject(altKey));
		assertEquals(altObject, env.getElementFromObject(altKey, "foo"));

	}

	@Test
	public void testPutWithPathDeep() {

		assertFalse(env.containsObject(altKey));
		assertNull(env.getObject(altKey));

		env.putObject(altKey, "foo.bar.flibble", altObject);

		assertTrue(env.containsObject(altKey));
		assertEquals(altObject, env.getElementFromObject(altKey, "foo.bar.flibble"));

	}

	@Test
	public void testPutWithPathIntoExisting() {

		assertFalse(env.containsObject(altKey));
		assertNull(env.getObject(altKey));

		env.putObject(testKey, "object.baz", altObject);

		assertEquals(altObject, env.getElementFromObject(testKey, "object.baz"));
		assertEquals(new JsonPrimitive(1234), env.getElementFromObject(testKey, "object.int"));

	}

	@Test
	public void testPutWithPathMultiple() {

		assertFalse(env.containsObject(altKey));
		assertNull(env.getObject(altKey));

		env.putObject(altKey, "foo.bar", altObject);

		assertEquals(altObject, env.getElementFromObject(altKey, "foo.bar"));
	}

	/**
	 * Test method for {@link Environment#putString(java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testPutString() {

		assertNull(env.getString(altKey));

		env.putString(altKey, altStringValue);

		assertEquals(altStringValue, env.getString(altKey));

	}

	/**
	 * Test method for {@link Environment#getString(java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testGetString_fromObject() {

		String testGet = env.getString(testKey, path);

		assertEquals("batman", testGet);

		String testNotFound = env.getString(testKey, pathNotFound);

		assertNull(testNotFound);

		String testObjectNotFound = env.getString(notFoundKey, path);

		assertNull(testObjectNotFound);

	}

	/**
	 * Test method for {@link Environment#getInteger(java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testGetInteger() {
		int expected = 1234;

		Integer actual = env.getInteger(testKey, intPath);

		assertNotNull(actual);
		assertEquals(expected, actual.intValue());

		assertNull(env.getInteger(testKey, pathNotFound));
	}

	@Test
	public void testGetStringAsInteger() {
		assertThrows(IllegalArgumentException.class, () -> {
			@SuppressWarnings("unused")
			Integer actual = env.getInteger(testKey, "stringint");
		});
	}

	@Test
	public void testGetStringAsBoolean() {
		assertThrows(IllegalArgumentException.class, () -> {
			@SuppressWarnings("unused")
			Boolean actual = env.getBoolean(testKey, "stringbool");
		});
	}

	/**
	 * Test method for {@link Environment#getLong(java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testGetLong() {
		long expected = 123465478745287987L;

		Long actual = env.getLong(testKey, longPath);

		assertNotNull(actual);
		assertEquals(expected, actual.longValue());

		assertNull(env.getLong(testKey, pathNotFound));
	}

	/**
	 * Test method for {@link Environment#getElementFromObject(java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testFindElement() {

		JsonElement arrayElement = env.getElementFromObject(testKey, "array");
		JsonElement objElement = env.getElementFromObject(testKey, "object");
		JsonElement notFound = env.getElementFromObject(testKey, notFoundKey);

		assertEquals(subArray, arrayElement);
		assertEquals(subObject, objElement);

		assertNull(notFound);

	}

	/**
	 * Test method for {@link Environment#getEffectiveKey(java.lang.String)}.
	 */
	@Test
	public void testMappedKeys() {
		assertEquals(mappedKey, env.getEffectiveKey(mappedKey));
		assertFalse(env.isKeyShadowed(testKey));
		assertFalse(env.isKeyMapped(mappedKey));

		env.mapKey(mappedKey, testKey);

		assertEquals(testKey, env.getEffectiveKey(mappedKey));
		assertTrue(env.isKeyShadowed(testKey));
		assertTrue(env.isKeyMapped(mappedKey));

		env.unmapKey(mappedKey);

		assertEquals(mappedKey, env.getEffectiveKey(mappedKey));
		assertFalse(env.isKeyShadowed(testKey));
		assertFalse(env.isKeyMapped(mappedKey));

	}

	/**
	 * Test method for {@link Environment#getEffectiveKey(java.lang.String)}.
	 */
	@Test
	public void testMappedObjects() {
		assertNull(env.getObject(mappedKey));
		assertFalse(env.containsObject(mappedKey));

		env.mapKey(mappedKey, testKey);

		assertEquals(testObject, env.getObject(mappedKey));
		assertTrue(env.containsObject(mappedKey));

		env.unmapKey(mappedKey);

		assertNull(env.getObject(mappedKey));
		assertFalse(env.containsObject(mappedKey));
	}

	@Test
	public void testShadowedObjects() {
		env.putObject(altKey, altObject);

		assertEquals(altObject, env.getObject(altKey));

		env.mapKey(altKey, testKey);

		assertEquals(testObject, env.getObject(altKey));

		env.unmapKey(altKey);

		assertEquals(altObject, env.getObject(altKey));
	}

}
