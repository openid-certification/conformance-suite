package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jayway.jsonpath.PathNotFoundException;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import com.jayway.jsonpath.JsonPath;
import net.openid.conformance.testmodule.OIDFJSON;

import static net.openid.conformance.testmodule.OIDFJSON.*;

public abstract class AbstractJsonAssertingCondition extends AbstractCondition {

	public abstract Environment evaluate(Environment environment);

	protected void assertHasField(JsonObject jsonObject, String path) {

		findByPath(jsonObject, path);

	}

	protected void assertJsonField(JsonObject jsonObject, String path, String expected) {
		JsonElement actual = findByPath(jsonObject, path);
		String stringValue = getOrFail(() -> getString(actual));
		if(!stringValue.equals(expected)) {
			throw error(String.format("Path %s did not match %s", path, expected), jsonObject);
		}
	}

	protected void assertJsonField(JsonObject jsonObject, String path, Number expected) {
		JsonElement actual = findByPath(jsonObject, path);
		Number number = getOrFail(() -> getNumber(actual));
		if(!number.equals(expected)) {
			throw error(String.format("Path %s did not match %s", path, expected), jsonObject);
		}
	}

	protected void assertJsonField(JsonObject jsonObject, String path, Character expected) {
		JsonElement actual = findByPath(jsonObject, path);
		Character c = getOrFail(() -> getCharacter(actual));
		if(!c.equals(expected)) {
			throw error(String.format("Path %s did not match %s", path, String.valueOf(expected)), jsonObject);
		}
	}

	protected void assertJsonField(JsonObject jsonObject, String path, boolean expected) {
		JsonElement actual = findByPath(jsonObject, path);
		Boolean bool = getOrFail(() -> getBoolean(actual));
		if(!bool.equals(expected)) {
			throw error(String.format("Path %s did not match %s", path, String.valueOf(expected)), jsonObject);
		}
	}

	private JsonElement findByPath(JsonObject jsonObject, String path) {
		try {
			return JsonPath.read(jsonObject, path);
		} catch (PathNotFoundException e) {
			throw error("Unable to find path " + path, jsonObject);
		}
	}

	private <T> T getOrFail(Lambda<T> lambda) {
		try {
			return lambda.execute();
		} catch (UnexpectedJsonTypeException u) {
			throw error("Wrong datatype being verified in json", u);
		}
	}

	@FunctionalInterface
	interface Lambda<T> {
		T execute();
	}
}
