package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.validation.Match;
import org.springframework.security.core.parameters.P;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static net.openid.conformance.testmodule.OIDFJSON.*;

public abstract class AbstractJsonAssertingCondition extends AbstractCondition {

	public abstract Environment evaluate(Environment environment);

	protected JsonObject bodyFrom(Environment environment) {
		String entityString = environment.getString("resource_endpoint_response");
		return new JsonParser().parse(entityString).getAsJsonObject();
	}

	protected JsonObject headersFrom(Environment environment) {
		return environment.getObject("resource_endpoint_response_headers");
	}

	protected void assertStatus(int expected, Environment environment) {
		int actual = statusFrom(environment);
		if(expected != actual) {
			throw error(String.format("Expected HTTP response code to be %d but it was %d", expected, actual));
		}
	}

	protected int statusFrom(Environment environment) {
		JsonObject responseCode = environment.getObject("resource_endpoint_response_code");
		JsonElement code = responseCode.get("code");
		return OIDFJSON.getInt(code);
	}

	protected void assertHasField(JsonObject jsonObject, String path) {

		findByPath(jsonObject, path);

	}

	protected void assertHasStringField(JsonObject jsonObject, String path) {
		JsonElement found = findByPath(jsonObject, path);
		try {
			OIDFJSON.getString(found);
		} catch(UnexpectedJsonTypeException u) {
			throw error("Field at " + path + " was not a string", jsonObject);
		}
	}

	protected void assertHasIntField(JsonObject jsonObject, String path) {
		JsonElement found = findByPath(jsonObject, path);
		try {
			OIDFJSON.getInt(found);
		} catch(UnexpectedJsonTypeException u) {
			throw error("Field at " + path + " was not an int", jsonObject);
		}
	}

	protected void assertHasDoubleField(JsonObject jsonObject, String path) {
		JsonElement found = findByPath(jsonObject, path);
		try {
			OIDFJSON.getDouble(found);
		} catch(UnexpectedJsonTypeException u) {
			throw error("Field at " + path + " was not a double", jsonObject);
		}
	}

	protected void assertHasFloatField(JsonObject jsonObject, String path) {
		JsonElement found = findByPath(jsonObject, path);
		try {
			OIDFJSON.getFloat(found);
		} catch(UnexpectedJsonTypeException u) {
			throw error("Field at " + path + " was not a float", jsonObject);
		}
	}

	protected void assertHasLongField(JsonObject jsonObject, String path) {
		JsonElement found = findByPath(jsonObject, path);
		try {
			OIDFJSON.getLong(found);
		} catch(UnexpectedJsonTypeException u) {
			throw error("Field at " + path + " was not a long", jsonObject);
		}
	}

	protected void assertHasBooleanField(JsonObject jsonObject, String path) {
		JsonElement found = findByPath(jsonObject, path);
		try {
			OIDFJSON.getBoolean(found);
		} catch(UnexpectedJsonTypeException u) {
			throw error("Field at " + path + " was not a boolean", jsonObject);
		}
	}

	protected void assertHasStringArrayField(JsonObject jsonObject, String path) {
		JsonElement found = findByPath(jsonObject, path);
		try {
			OIDFJSON.getStringArray(found);
		} catch(UnexpectedJsonTypeException u) {
			throw error("Field at " + path + " was not an array of strings", jsonObject);
		}
	}

	protected void assertHasCharField(JsonObject jsonObject, String path) {
		JsonElement found = findByPath(jsonObject, path);
		try {
			OIDFJSON.getCharacter(found);
		} catch(UnexpectedJsonTypeException u) {
			throw error("Field at " + path + " was not a character", jsonObject);
		}
	}

	protected void assertHasShortField(JsonObject jsonObject, String path) {
		JsonElement found = findByPath(jsonObject, path);
		try {
			OIDFJSON.getShort(found);
		} catch(UnexpectedJsonTypeException u) {
			throw error("Field at " + path + " was not a short", jsonObject);
		}
	}

	protected void assertHasByteField(JsonObject jsonObject, String path) {
		JsonElement found = findByPath(jsonObject, path);
		try {
			OIDFJSON.getByte(found);
		} catch(UnexpectedJsonTypeException u) {
			throw error("Field at " + path + " was not a byte", jsonObject);
		}
	}

	protected void assertJsonField(JsonObject jsonObject, String path, String expected) {
		JsonElement actual = findByPath(jsonObject, path);
		String stringValue = getOrFail(() -> getString(actual));
		if(!stringValue.equals(expected)) {
			throw error(String.format("Path %s did not match %s", path, expected), jsonObject);
		}
	}

	protected void assertJsonField(JsonObject jsonObject, String path, Match match) {
		JsonElement actual = findByPath(jsonObject, path);
		String stringValue = "";
		try {
			stringValue = getString(actual);
		} catch (UnexpectedJsonTypeException e) {
			try {
				stringValue = String.valueOf(getNumber(actual));
			} catch (UnexpectedJsonTypeException ex) {
				throw error(String.format("Path %s was not a string or number", path), jsonObject);
			}
		}
		if(!match.matches(stringValue)) {
			throw error(String.format("Path %s did not match %s", path, match), jsonObject);
		}
	}

	protected void assertJsonField(JsonObject jsonObject, String path, String... expected) {
		JsonElement actual = findByPath(jsonObject, path);
		List<String> array = getOrFail(() -> getStringArray(actual));
		List<String> found = Arrays.stream(expected)
			.filter(s -> !array.contains(s))
			.collect(Collectors.toList());
		if(found.size() != 0) {
			throw error(String.format("Headers did not contain all of %s", String.join(" ", expected)), jsonObject);
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
