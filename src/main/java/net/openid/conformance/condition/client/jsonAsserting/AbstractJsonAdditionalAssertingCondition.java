package net.openid.conformance.condition.client.jsonAsserting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.field.Field;
import net.openid.conformance.validation.Match;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractJsonAdditionalAssertingCondition extends AbstractJsonAssertingCondition {

	@Override
	public abstract Environment evaluate(Environment env);

	public void assertStatus(int expected, Environment environment) {
		int actual = statusFrom(environment);
		if (expected != actual) {
			throw error(String.format("Expected HTTP response code to be %d but it was %d", expected, actual));
		}
	}

	private int statusFrom(Environment environment) {
		JsonObject responseCode = environment.getObject("resource_endpoint_response_code");
		JsonElement code = responseCode.get("code");
		return OIDFJSON.getInt(code);
	}

	protected void assertCurrencyType(JsonElement jsonObject, Field field) {
		assertHasStringField(jsonObject, field.getPath());
		JsonElement found = findByPath(jsonObject, field.getPath());
		String value = getJsonValueAsString(found, field.getPath());
		assertCurrencyNotNa(value, field);
		assertField(jsonObject, field);
	}

	public void assertJsonField(JsonElement jsonObject, String path, String expected) {
		JsonElement actual = findByPath(jsonObject, path);
		String stringValue = getOrFail(() -> OIDFJSON.getString(actual));
		if (!stringValue.equals(expected)) {
			throw error(String.format("Path %s did not match %s", path, expected),
				args("value", jsonObject));
		}
	}

	protected void assertJsonField(JsonElement jsonObject, String path, String... expected) {
		JsonElement actual = findByPath(jsonObject, path);
		List<String> array = getOrFail(() -> OIDFJSON.getStringArray(actual));
		List<String> found = Arrays.stream(expected)
			.filter(s -> !array.contains(s))
			.collect(Collectors.toList());
		if (found.size() != 0) { //NOPMD
			throw error(String.format("Headers did not contain all of %s", String.join(" ", expected)),
				args("value", jsonObject));
		}
	}

	protected void assertJsonField(JsonElement jsonObject, String path, Match match) {
		JsonElement found = findByPath(jsonObject, path);
		String stringValue = getJsonValueAsString(found, path);
		if (!match.matches(stringValue)) {
			throw error(String.format("Path %s did not match %s", path, match),
				args("value", jsonObject));
		}
	}

	protected void assertJsonField(JsonElement jsonObject, String path, Number expected) {
		JsonElement actual = findByPath(jsonObject, path);
		Number number = getOrFail(() -> OIDFJSON.getNumber(actual));
		if (!number.equals(expected)) {
			throw error(String.format("Path %s did not match %s", path, expected),
				args("value", jsonObject));
		}
	}

	protected void assertJsonField(JsonElement jsonObject, String path, Character expected) {
		JsonElement actual = findByPath(jsonObject, path);
		Character c = getOrFail(() -> OIDFJSON.getCharacter(actual));
		if (!c.equals(expected)) {
			throw error(String.format("Path %s did not match %s", path, String.valueOf(expected)),
				args("value", jsonObject));
		}
	}

	protected void assertJsonField(JsonElement jsonObject, String path, boolean expected) {
		JsonElement actual = findByPath(jsonObject, path);
		Boolean bool = getOrFail(() -> OIDFJSON.getBoolean(actual));
		if (!bool.equals(expected)) {
			 throw error(String.format("Path %s did not match %s", path, String.valueOf(expected)),
				 args("value", jsonObject));
		}
	}

	private <T> T getOrFail(Lambda<T> lambda) {
		try {
			return lambda.execute();
		} catch (OIDFJSON.UnexpectedJsonTypeException u) {
			throw error("Wrong datatype being verified in json", u);
		}
	}

	@FunctionalInterface
	interface Lambda<T> {
		T execute();
	}
}
