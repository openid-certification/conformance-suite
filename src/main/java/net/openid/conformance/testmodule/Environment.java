package net.openid.conformance.testmodule;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * An element for storing the current running state of a test module in a way that it can be passed around.
 *
 * The Environment stores JSON objects indexed by strings. Items in the JSON objects themselves can be indexed by foo.bar.baz path
 * selectors using the getString(String, String) and getElement(String, String) functions.
 *
 * Object keys can be mapped, such that one key shadows another stored object. For example, if the store originally looks like:
 *
 *   foo: { bar: 1234 },
 *   baz: { qux: 9876 }
 *
 * Calls to getObject(foo) will return { bar : 1234 }
 *
 * If the key "baz" is mapped over the key "foo" with mapKey(foo, baz), this points all references to the key "foo" to use the
 * existing "baz" object the store effectively looks like:
 *
 *   <hidden foo>: { bar: 1234 },
 *   foo <mapped over baz>: { qux: 9876 }
 *
 * Any calls to object functions using "foo" such as getObject(foo) will return { qux: 9876 }. Any calls to "baz" will still reference { qux: 9876 }.
 * The original object of "foo" { bar: 1234 } is effectively unreachable until "foo" is unmapped.
 *
 * Native values (strings, integers, longs) can be stored and accessed through special accessor functions. These values are
 * stored internally in a dedicated JSON object alongside all other objects. A native value and a JSON object can be stored using
 * the same key, but the two of these are unrelated to each other. Native value keys are never mapped.
 *
 */
public class Environment {

	// used for serializing as a json object in "toString"
	private static final Type MAP_STRING_STRING_TYPE = new TypeToken<Map<String, String>>() {}.getType();
	private static final Type MAP_STRING_JSONOBJECT_TYPE = new TypeToken<Map<String, JsonObject>>() {}.getType();
	private static final Gson gson = new Gson();

	/**
	 * Set up a lock for threading purposes
	 */
	private ReentrantLock lock = new ReentrantLock(true); // set with fairness policy to get up control to the longest waiting thread

	/**
	 * Holds the named j.u.c.l.conditions
	 */
	private Map<String, Condition> lockConditions = new HashMap<>();

	// key for storing native values directly
	private static final String NATIVE_VALUES = "_NATIVE_VALUES";
	private Map<String, JsonObject> store = Maps.newHashMap(
		ImmutableMap.of(NATIVE_VALUES, new JsonObject())); // make sure we start with a place to putObject the string values

	private Map<String, String> keyMap = new HashMap<>();


	/**
	 * Check to see if there is an object in the Environment referenced by the given key. If
	 * the key has been mapped to another key, the mapped key is dereferenced first and the
	 * mapped key is used for the check instead. Any object shadowed by the mapped key reimains
	 * untouched.
	 *
	 * This does not check the native value store.
	 *
	 * @param key the object ID to search for, will be checked against any mapped keys
	 * @return true if the store contains an object with this key, false otherwise
	 */
	public boolean containsObject(String key) {
		return store.containsKey(getEffectiveKey(key));
	}

	/**
	 * Get a JSON object from the Environment store, if one exists under this key. If the
	 * key has been mapped to another key, the mapped key is dereferenced first and the mapped
	 * key is used for the fetch instead. Any object shadowed by the mapped key remains untouched.
	 *
	 * This does not check the native value store.
	 *
	 * @param key the object ID to fetch, will be checked against any mapped keys
	 * @return the stored object if it exists, null if it does not
	 */
	public JsonObject getObject(String key) {
		return store.get(getEffectiveKey(key));
	}

	/**
	 * Remove a JSON object from this environment, if it exists. If the key has been mapped to
	 * another key, the mapped key is dereferenced first and the mapped key is used for the
	 * removal instead. Any object shadowed by the mapped key remains untouched.
	 *
	 * This does not affect the native value store.
	 *
	 * @param key the object ID to removeObject, will be checked against any mapped keys
	 */
	public void removeObject(String key) {
		store.remove(getEffectiveKey(key));
	}


	/**
	 * Put an object into the Environment, overwriting an existing object if one is there.
	 * If the key has been mapped to another key, the mapped key is dereferenced first and
	 * mapped key is used for the insertion. Any object shadowed by the mapped key remains
	 * untouched.
	 *
	 * This does not affect the native value store.
	 *
	 * @param key the key to store the object under; this may be mapped
	 * @param value the object to store
	 * @return the stored object
	 */
	public JsonObject putObject(String key, JsonObject value) {
		return store.put(getEffectiveKey(key), value);
	}

	private void putElement(String key, String path, JsonElement value) {
		JsonObject o = getObject(key);
		if (o == null) {
			o = new JsonObject();
			putObject(key, o);
		}

		ArrayList<String> pathSegments = Lists.newArrayList(Splitter.on('.').split(path));
		int lastIndex = pathSegments.size() - 1;
		String lastSegment = pathSegments.get(lastIndex);
		pathSegments.remove(lastIndex);

		for (String pathSegment: pathSegments) {
			JsonElement nextO = o.get(pathSegment);
			if (nextO == null) {
				nextO = new JsonObject();
				o.add(pathSegment, nextO);
			} else if (nextO.isJsonObject()) {
				// object already exists
			} else {
				throw new UnexpectedTypeException("putObject(%s, %s, obj) found a non-object of type %s in the path at %s".formatted(
					key, path, nextO.getClass().getSimpleName(), pathSegment));
			}
			o = (JsonObject) nextO;
		}
		o.add(lastSegment, value);
	}

	public void putObject(String key, String path, JsonObject value) {
		putElement(key, path, value);
	}

	public void putArray(String key, String path, JsonArray value) {
		putElement(key, path, value);
	}

	public void putString(String key, String path, String value) {
		putElement(key, path, new JsonPrimitive(value));
	}

	public void removeElement(String key, String path) {

		JsonObject o = getObject(key);
		if (o == null) {
			throw new NoSuchElementException("No object with key %s found in path %s".formatted(key, path));
		}

		ArrayList<String> pathSegments = Lists.newArrayList(Splitter.on('.').split(path));
		int lastIndex = pathSegments.size() - 1;
		String lastSegment = pathSegments.get(lastIndex);
		pathSegments.remove(lastIndex);

		for (String pathSegment: pathSegments) {
			JsonElement nextO = o.get(pathSegment);
			if (nextO == null) {
				throw new NoSuchElementException("No object with key %s found in path %s".formatted(key, path));
			} else if (nextO.isJsonObject()) {
				// object already exists
			} else {
				throw new UnexpectedTypeException("putObject(%s, %s, obj) found a non-object of type %s in the path at %s".formatted(
					key, path, nextO.getClass().getSimpleName(), pathSegment));
			}
			o = (JsonObject) nextO;
		}
		o.remove(lastSegment);
	}

	public JsonObject putObjectFromJsonString(String key, String json) {
		return putObject(key, JsonParser.parseString(json).getAsJsonObject());
	}

	public void putObjectFromJsonString(String key, String path, String json) {
		JsonObject newObj = JsonParser.parseString(json).getAsJsonObject();
		putObject(key, path, newObj);
	}

	/**
	 * Get a sub-element from a JSON object within the environment, if that object exists. Any
	 * JSON element can be returned from this function, including objects, arrays, literals, and
	 * JSON nulls.
	 *
	 * The path elements are separated by ".", so with the following object:
	 *
	 * {
	 *  foo: {
	 *    bar: {
	 *      baz: "value"
	 *    }
	 *  }
	 * }
	 *
	 * The path "foo.bar.baz" is the string literal "value", while the path "foo.bar" is the
	 * object { baz: "value" }.
	 *
	 * If the object identified by the key is not found, null is returned. If the object
	 * does not contain any element represented by the path, null is returned.
	 *
	 * @param key the object identifier to look up, may be mapped; see getObject(String)
	 * @param path the path within the object to search; in dot-separated notation
	 * @return the element within the object if found, null if the object is not found, or null if no element is found at the given path within the object
	 */
	public JsonElement getElementFromObject(String key, String path) {

		// get the object we're looking for and recursively start our walk here
		JsonElement e = getObject(key);

		if (e == null) {
			return null;
		}

		Iterable<String> parts = Splitter.on('.').split(path);
		Iterator<String> it = parts.iterator();

		while (it.hasNext()) {
			String p = it.next();
			if (e.isJsonObject()) {
				JsonObject o = e.getAsJsonObject();
				if (o.has(p)) {
					e = o.get(p); // found the next level
					if (!it.hasNext()) {
						// we've reached a leaf at the right part of the key, return what we found
						return e;
					}
				} else {
					// didn't find it, stop processing
					break;
				}
			} else {
				throw new UnexpectedTypeException("An object is required for %s.%s but %s was found whilst traversing the path".formatted(
					key, path, e.getClass().getSimpleName()));
			}
		}

		// didn't find it
		return null;

	}

	/**
	 * Gets a sub-element from a named object at the given path and returns it as a native String,
	 * if it is stored as one.
	 *
	 * This does not access the native values store.
	 *
	 * See getElementFromObject(String, String)
	 *
	 * @param key the object identifier to look up, may be mapped; see getObject(String)
	 * @param path the path within the object to search; in dot-separated notation
	 * @return the value within the object if found, null if the object is not found,
	 * 	null if no element is found at the given path within the object
	 * @throws UnexpectedTypeException if the element is not the appropriate native type
	 */
	public String getString(String key, String path) {
		JsonElement e = getElementFromObject(key, path);
		if (e == null) {
			// we didn't find it
			return null;
		}

		if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isString()) {
			return OIDFJSON.getString(e);
		} else {
			throw new UnexpectedTypeException("If present, a string is expected for %s %s but %s was found".formatted(
				key, path, e.getClass().getSimpleName()));
		}
	}

	/**
	 * Gets a sub-element from a named object at the given path and returns it as a native Integer,
	 * if it is stored as one.
	 *
	 * This does not access the native values store.
	 *
	 * See getElementFromObject(String, String)
	 *
	 * @param key the object identifier to look up, may be mapped; see getObject(String)
	 * @param path the path within the object to search; in dot-separated notation
	 * @return the value within the object if found, null if the object is not found,
	 * 	null if no element is found at the given path within the object
	 * @throws UnexpectedTypeException if the element is not the appropriate native type
	 */
	public Integer getInteger(String key, String path) {
		JsonElement e = getElementFromObject(key, path);
		if (e == null) {
			return null;
		}

		if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber()) {
			return OIDFJSON.getNumber(e).intValue();
		} else {
			throw new UnexpectedTypeException("A number is required for %s %s but %s was found".formatted(
				key, path, e.getClass().getSimpleName()));
		}
	}


	/**
	 * Gets a sub-element from a named object at the given path and returns it as a native Boolean,
	 * if it is stored as one.
	 *
	 * This does not access the native values store. ( see wrapper function )
	 *
	 * See getElementFromObject(String, String)
	 *
	 * @param key the object identifier to look up, may be mapped; see getObject(String)
	 * @param path the path within the object to search; in dot-separated notation
	 * @return the value within the object if found, null if the object is not found,
	 * 	null if no element is found at the given path within the object
	 * @throws UnexpectedTypeException if the element is not the appropriate native type
	 */

	public Boolean getBoolean(String key, String path) {
		JsonElement e = getElementFromObject(key, path);
		if (e == null) {
			return null;
		}
		if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isBoolean()) {
			return OIDFJSON.getBoolean(e);
		} else {
			throw new UnexpectedTypeException("A number is required for %s %s but %s was found".formatted(
				key, path, e.getClass().getSimpleName()));
		}
	}


	/**
	 * Gets a sub-element from a named object at the given path and returns it as a native Long,
	 * if it is stored as one.
	 *
	 * This does not access the native values store.
	 *
	 * See getElementFromObject(String, String)
	 *
	 * @param key the object identifier to look up, may be mapped; see getObject(String)
	 * @param path the path within the object to search; in dot-separated notation
	 * @return the value within the object if found, null if the object is not found,
	 * 	null if no element is found at the given path within the object
	 * @throws UnexpectedTypeException if the element is not the appropriate native type
	 */
	public Long getLong(String key, String path) {
		JsonElement e = getElementFromObject(key, path);
		if (e == null) {
			return null;
		}
		if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber()) {
			return OIDFJSON.getNumber(e).longValue();
		} else {
			throw new UnexpectedTypeException("A number is required for %s %s but %s was found".formatted(
				key, path, e.getClass().getSimpleName()));
		}
	}


	/*
	 * prints out the environment as a mostly-json-formatted string
	 */
	@Override
	public String toString() {
		return "Environment: { \"store\" : " + gson.toJson(store, MAP_STRING_JSONOBJECT_TYPE)
			+ ", \"keyMap\" : " + gson.toJson(keyMap, MAP_STRING_STRING_TYPE) + " }";
	}

	/**
	 * Get the lock used for synchronization of a running test instance.
	 *
	 * @return the lock
	 */
	public ReentrantLock getLock() {
		return lock;
	}

	/**
	 * Executes the given Runnable while holding the {@link Environment#lock}.
	 * @param runnable
	 */
	public void doWithLock(Runnable runnable) {
		lock.lock();
		try {
			runnable.run();
		} finally {
			lock.unlock();
		}
	}

	/**
	 * If the key is mapped to another value, get the underlying value. Otherwise return the input key itself.
	 *
	 * This lookup does not chain to multiple levels -- if "to" is itself a mapping to something else and does not otherwise
	 * exist in the environment, its value will not be found.
	 *
	 * @param key
	 * @return
	 */
	public String getEffectiveKey(String key) {
		if (keyMap.containsKey(key)) {
			return keyMap.get(key);
		} else {
			return key;
		}
	}

	/**
	 * Add a mapping from one key value to another. When things are looked up by "from" it will look for "to" in the storage instead.
	 *
	 * This lookup does not chain to multiple levels -- if "to" is itself a mapping to something else and does not otherwise
	 * exist in the environment, its value will not be found.
	 *
	 * For example, if the store originally looks like:
	 *
	 *   foo: { bar: 1234 },
	 *   baz: { qux: 9876 }
	 *
	 * Calls to getObject(foo) will return { bar : 1234 }
	 *
	 * If the key "baz" is mapped over the key "foo" with mapKey(foo, baz), this points all references to the key "foo" to use the
	 * existing "baz" object the store effectively looks like:
	 *
	 *   <hidden foo>: { bar: 1234 },
	 *   foo <mapped over baz>: { qux: 9876 }
	 *
	 * Any calls to object functions using "foo" such as getObject(foo) will return { qux: 9876 }. Any calls to objects functions using
	 * "baz" will still access the original object for "baz", such as getObject(baz) will return { qux: 9876 }.
	 *
	 * @param from the key to map over "to", which will be used by outside calls until it is unmapped
	 * @param to the key that "from" is mapped on top of; this key is shadowed by "from"
	 * @return the previously mapped "to" or "null" if not mapped
	 */
	public String mapKey(String from, String to) {
		return keyMap.put(from, to);
	}

	/**
	 * Remove a mapped key. This restores the default behavior.
	 *
	 * For example, if the store originally looks like:
	 *
	 *   <hidden foo>: { bar: 1234 },
	 *   foo <mapped over baz>: { qux: 9876 }
	 *
	 * Calls to getObject(foo) will return { qux: 9876 }
	 *
	 * If the key "foo" unmapped, the store will look like:
	 *
	 *   foo: { bar: 1234 },
	 *   baz: { qux: 9876 }
	 *
	 * Any calls to object functions using "foo" such as getObject(foo) will return { qux: 9876 }
	 *
	 * @param key the key to removeObject a mapping about, this is the "from" used in mapKey
	 * @return the previously mapped key or "null" if not mapped
	 */
	public String unmapKey(String key) {
		return keyMap.remove(key);
	}

	/**
	 * Test if a key has had another key mapped over it. Note that calling an access function on a shadowed key will
	 * return the same object as an unshadowed key.
	 *
	 * For example, if the environment contains:
	 *
	 *   <hidden foo>: { bar: 1234 },
	 *   foo <mapped over baz>: { qux: 9876 }
	 *
	 * Then isKeyShadowed(baz) will return true. Note that isKeyShadowed(foo) will return false in this specific example, though
	 * it could itself be shadowed by something else.
	 *
	 * @param key the key to check if it's being shadowed by another key; corresponds to the "to" value in mapKey(String, String)
	 * @return true if the key is shadowed by another, false otherwise
	 */
	public boolean isKeyShadowed(String key) {
		return keyMap.containsValue(key);
	}

	/**
	 * Test if a given key is mapped to another value. Calling an access function with a mapped key
	 * will return the value of the target of the map and not any original values for this key which may also
	 * exist in the environment.
	 *
	 * For example, if the environment contains:
	 *
	 *   <hidden foo>: { bar: 1234 },
	 *   foo <mapped over baz>: { qux: 9876 }
	 *
	 * Then isKeyMapped(foo) returns true. Note that isKeyMapped(baz) will return false in this specific example, though it
	 * could itself be mapped to something else.
	 *
	 * @param key the key to check if it's being mapped to another key; corresponds to the "from" value in mapKey(String, String)
	 * @return true if the key is mapped to another key, false otherwise
	 */
	public boolean isKeyMapped(String key) {
		return keyMap.containsKey(key);
	}


	//
	// Native value accessor functions
	//


	/**
	 * Retrieve a String from the native value store.
	 *
	 * @param key the ID of the value to search for; this is not mapped
	 * @return the value if it's found, null otherwise
	 */
	public String getString(String key) {
		return getString(NATIVE_VALUES, key);
	}

	/**
	 * Retrieve an Integer from the native value store.
	 *
	 * @param key the ID of the value to search for; this is not mapped
	 * @return the value if it's found, null otherwise
	 */
	public Integer getInteger(String key) {
		return getInteger(NATIVE_VALUES, key);
	}

	/**
	 * Retrieve a Boolean from the native value store.
	 *
	 * @param key the ID of the value to search for; this is not mapped
	 * @return the value if it's found, null otherwise
	 */

	public Boolean getBoolean(String key) {
		return getBoolean(NATIVE_VALUES,key);
	}

	/**
	 * Retrieve a Long from the native value store.
	 *
	 * @param key the ID of the value to search for; this is not mapped
	 * @return the value if it's found, null otherwise
	 */
	public Long getLong(String key) {
		return getLong(NATIVE_VALUES, key);
	}

	/**
	 * Put a Long into the native value store.
	 *
	 * @param key the key by which the value can be retrieved from the native value store
	 * @param value the value to store
	 */
	public void putLong(String key, Long value) {
		JsonObject o = getObject(NATIVE_VALUES);
		o.addProperty(key, value);
	}

	/**
	 * Put an Integer into the native value store.
	 *
	 * @param key the key by which the value can be retrieved from the native value store
	 * @param value the value to store
	 */
	public void putInteger(String key, Integer value) {
		JsonObject o = getObject(NATIVE_VALUES);
		o.addProperty(key, value);
	}

	/**
	 * Put an Boolean into the native value store.
	 *
	 * @param key the key by which the value can be retrieved from the native value store
	 * @param value the value to store
	 */
	public void putBoolean(String key, Boolean value) {
		JsonObject o = getObject(NATIVE_VALUES);
		o.addProperty(key, value);
	}

	/**
	 * Put a String into the native objects store.
	 *
	 * @param key the key by which the value can be retrieved from the native value store
	 * @param value the value to store
	 */
	public void putString(String key, String value) {
		JsonObject o = getObject(NATIVE_VALUES);
		o.addProperty(key, value);
	}

	/**
	 * Remove a value from the native objects store, if it exists.
	 *
	 * @param key the key by which the value is known in the native value store
	 */
	public void removeNativeValue(String key) {
		JsonObject natives = getObject(NATIVE_VALUES);
		natives.remove(key);
	}

	/**
	 * Registers a new {@link Condition} with the given {@code conditionKey}.
	 * @param conditionKey
	 */
	public void registerLockCondition(String conditionKey) {
		lockConditions.put(conditionKey, lock.newCondition());
	}

	/**
	 * Awaits a signal from the {@link Condition} with the given {@code conditionKey}.
	 *
	 * @param conditionKey
	 * @param time
	 * @param timeUnit
	 * @return
	 */
	@SuppressWarnings("all")
	public boolean awaitLockCondition(String conditionKey, long time, TimeUnit timeUnit) {
		Condition condition = lockConditions.get(conditionKey);
		if (condition == null) {
			throw new RuntimeException("Condition '" + conditionKey + "' not found");
		}
		try { // awaitCondition is called in a loop
			return condition.await(time, timeUnit);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Signal the {@link Condition} with the given {@code conditionKey}.
	 *
	 * @param conditionKey
	 */
	public void signalLockCondition(String conditionKey) {
		Condition condition = lockConditions.get(conditionKey);
		if (condition == null) {
			throw new RuntimeException("Condition '" + conditionKey + "' not found");
		}
		condition.signalAll();
	}

	/**
	 * To allow conditions catch these exceptions when necessary
	 * i.e to catch and throw a nicer 'error(..., args(...))' from a condition
	 */
	@SuppressWarnings("serial")
	public static class UnexpectedTypeException extends IllegalArgumentException {
		public UnexpectedTypeException(String msg) {
			super(msg);
		}
	}

}
