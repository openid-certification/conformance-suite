/*******************************************************************************
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package io.fintechlabs.testframework.testmodule;

import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author jricher
 *
 */
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
	@Before
	public void setUp() throws Exception {
		
		env = new Environment();
		
		testKey = "test";
		
		notFoundKey = "not_found";
		
		altKey = "alt";
		
		mappedKey = "mapped";
		
		testObject = new JsonParser().parse("{\n" + 
			"	\"long\": 123465478745287987,\n" + 
			"	\"string\": \"value\",\n" + 
			"	\"array\": [1, 2, \"a\", \"b\"],\n" + 
			"	\"object\": {\n" + 
			"		\"int\": 1234,\n" +
			"		\"foo\": \"bar\",\n" + 
			"		\"baz\": {\n" + 
			"			\"qux\": \"batman\"\n" + 
			"		}\n" + 
			"	}\n" + 
			"}").getAsJsonObject();
		
		subArray = new JsonParser().parse("[1, 2, \"a\", \"b\"]").getAsJsonArray();
		
		subObject = new JsonParser().parse("{\n" + 
			"		\"int\": 1234,\n" +
			"		\"foo\": \"bar\",\n" + 
			"		\"baz\": {\n" + 
			"			\"qux\": \"batman\"\n" + 
			"		}\n" + 
			"	}\n").getAsJsonObject();
		
		path = "object.baz.qux";
		
		longPath = "long";
		
		intPath = "object.int";
		
		pathNotFound = "apple.banana.republic";
		
		altObject = new JsonParser().parse("{\n" + 
			"	\"number\": 9876,\n" + 
			"	\"thing\": \"evaluation\",\n" + 
			"	\"list\": [10, 20, \"z\", \"w\"],\n" + 
			"	\"box\": {\n" + 
			"		\"bat\": \"man\",\n" + 
			"		\"man\": {\n" + 
			"			\"batman\": \"yes\"\n" + 
			"		}\n" + 
			"	}\n" + 
			"}").getAsJsonObject();
		
		
		testStringKey = "test_string";
		
		testStringValue = "Test String Value";
		
		altStringValue = "Alternative String Value";
		
		env.put(testKey, testObject);
		env.putString(testStringKey, testStringValue);
		
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.testmodule.Environment#containsObj(java.lang.String)}.
	 */
	@Test
	public void testContainsObj() {

		assertTrue(env.containsObj(testKey));
		assertFalse(env.containsObj(notFoundKey));
		
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.testmodule.Environment#get(java.lang.String)}.
	 */
	@Test
	public void testGet() {
		
		JsonObject testGet = env.get(testKey);
		
		assertEquals(testObject, testGet);
		
		JsonObject notFoundGet = env.get(notFoundKey);
		
		assertNull(notFoundGet);
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.testmodule.Environment#remove(java.lang.String)}.
	 */
	@Test
	public void testRemove() {

		// add a secondary object
		env.put(altKey, altObject);

		env.remove(testKey);
		
		assertFalse(env.containsObj(testKey));
		assertNull(env.get(testKey));
		
		// make sure other object is not altered
		assertEquals(altObject, env.get(altKey));
	
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.testmodule.Environment#getString(java.lang.String)}.
	 */
	@Test
	public void testGetStringString() {

		String stringGet = env.getString(testStringKey);
		
		assertEquals(testStringValue, stringGet);
		
		String notFoundGet = env.getString(notFoundKey);
		
		assertNull(notFoundGet);

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.testmodule.Environment#put(java.lang.String, com.google.gson.JsonObject)}.
	 */
	@Test
	public void testPut() {

		assertFalse(env.containsObj(altKey));
		assertNull(env.get(altKey));
		
		env.put(altKey, altObject);
		
		assertTrue(env.containsObj(altKey));
		assertEquals(altObject, env.get(altKey));
		
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.testmodule.Environment#putString(java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testPutString() {

		assertNull(env.getString(altKey));
		
		env.putString(altKey, altStringValue);
		
		assertEquals(altStringValue, env.getString(altKey));
	
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.testmodule.Environment#getString(java.lang.String, java.lang.String)}.
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
	 * Test method for {@link io.fintechlabs.testframework.testmodule.Environment#getInteger(java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testGetInteger() {
		int expected = 1234;
		
		Integer actual = env.getInteger(testKey, intPath);
		
		assertNotNull(actual);
		assertEquals(expected, actual.intValue());
		
		assertNull(env.getInteger(testKey, pathNotFound));
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.testmodule.Environment#getLong(java.lang.String, java.lang.String)}.
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
	 * Test method for {@link io.fintechlabs.testframework.testmodule.Environment#findElement(java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testFindElement() {

		JsonElement arrayElement = env.findElement(testKey, "array");
		JsonElement objElement = env.findElement(testKey, "object");
		JsonElement notFound = env.findElement(testKey, notFoundKey);
		
		assertEquals(subArray, arrayElement);
		assertEquals(subObject, objElement);
		
		assertNull(notFound);
	
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.testmodule.Environment#getEffectiveKey(java.lang.String)}.
	 */
	@Test
	public void testMappedKeys() {
		assertEquals(mappedKey, env.getEffectiveKey(mappedKey));
		assertFalse(env.isKeyMapped(testKey));
		
		env.mapKey(mappedKey, testKey);
		
		assertEquals(testKey, env.getEffectiveKey(mappedKey));
		assertTrue(env.isKeyMapped(testKey));
		
		env.unmapKey(mappedKey);
		
		assertEquals(mappedKey, env.getEffectiveKey(mappedKey));
		assertFalse(env.isKeyMapped(testKey));

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.testmodule.Environment#getEffectiveKey(java.lang.String)}.
	 */
	@Test
	public void testMappedObjects() {
		assertNull(env.get(mappedKey));
		
		env.mapKey(mappedKey, testKey);
		
		assertEquals(testObject, env.get(mappedKey));
		
		env.unmapKey(mappedKey);
		
		assertNull(env.get(mappedKey));
	}

}
