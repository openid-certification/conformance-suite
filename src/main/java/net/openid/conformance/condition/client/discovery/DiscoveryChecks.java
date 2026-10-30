package net.openid.conformance.condition.client.discovery;


import com.google.gson.JsonElement;
import net.openid.conformance.condition.FunctionalCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public class DiscoveryChecks {

	public static FunctionalCondition.Body propertyHasValue(String propertyPath, Function<Environment, String> expectedValue) {
		return (env, c) -> {
			String expected = expectedValue.apply(env);

			JsonElement value = env.getElementFromObject("server", propertyPath);
			if (value == null || !(OIDFJSON.isString(value) || value.isJsonArray())) {
				throw c.error(propertyPath  + " is missing from the discovery document or is not a string",
					Map.of(propertyPath, String.valueOf(value)));
			}
			if (value.isJsonArray()) {
				List<String> actualList = OIDFJSON.convertJsonArrayToList(value.getAsJsonArray());
				if (!actualList.contains(expected)) {
					throw c.error(propertyPath + " in the discovery document does not contain expected value",
						Map.of("expected", expected, "actual", actualList));
				}
				c.logSuccess(propertyPath + " in the discovery document contain the expected value", Map.of("expected", expected, "actual", actualList));

			} else {
				String actual = OIDFJSON.getString(value);
				if (!actual.equals(expected)) {
					throw c.error(propertyPath + " in the discovery document is not identical to expected value",
						Map.of("expected", expected, "actual", actual));
				}
				c.logSuccess(propertyPath + " in the discovery document is identical to the expected value", Map.of(propertyPath, actual));
			}
		};
	}

	public static FunctionalCondition.Body propertyNotHasAnyValue(String propertyPath, Function<Environment, List<String>> deniedValue) {
		return (env, c) -> {
			List<String> expected = deniedValue.apply(env);

			JsonElement value = env.getElementFromObject("server", propertyPath);
			if (value == null ) {
				c.logSuccess(propertyPath + " is not advertised by discovery document" );
				return;
			}
			if (value.isJsonArray() || OIDFJSON.isString(value)) {
				List<String> vals = value.isJsonArray() ? OIDFJSON.convertJsonArrayToList(value.getAsJsonArray()) : List.of(OIDFJSON.getString(value));

				List<String> intersection = expected.stream()
					.filter(vals::contains)
					.toList();

				if (!intersection.isEmpty()) {
					throw c.error(propertyPath + " in the discovery document does contain denied value",
						Map.of("denied", expected, "actual", vals));
				}

				c.logSuccess(propertyPath + " in the discovery document does not contain a denied value", Map.of("denied", expected, "actual", vals));

			} else {
				throw c.error(propertyPath + " in the discovery document has a value that is not expected",
					Map.of( "actual", value));
			}
		};
	}

	public static FunctionalCondition.Body isOneOf(String propertyPath, Function<Environment, List<String>> expectedValue) {
		return (env, c) -> {
			List<String> expected = expectedValue.apply(env);

			JsonElement value = env.getElementFromObject("server", propertyPath);
			if (value == null || !OIDFJSON.isString(value)) {
				throw c.error(propertyPath  + " is missing from the discovery document or is not a string",
					Map.of(propertyPath, String.valueOf(value)));
			}
			String actual = OIDFJSON.getString(value);

			if (!expected.contains(actual)) {
				throw c.error(propertyPath + " in the discovery document is not a valid expected value",
					Map.of("expected", expected, "actual", actual));
			}
			c.logSuccess(propertyPath + " in the discovery document has a valid value", Map.of(propertyPath, actual));
		};
	}

	public static Consumer<Environment> extractStringValue(String propertyPath, String envName) {
		return (env) -> {
			JsonElement value = env.getElementFromObject("server", propertyPath);
			if (value == null || !OIDFJSON.isString(value)) {
				return;
			}
			env.putString(envName, OIDFJSON.getString(value));
		};
	}

}
