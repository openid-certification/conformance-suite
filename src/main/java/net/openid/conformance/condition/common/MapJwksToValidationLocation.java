package net.openid.conformance.condition.common;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Copies the JWK set to be validated from its source location into the common
 * {@code jwks_to_validate} environment key, so the generic JWKS-validation conditions can run
 * against any source. The source object key, an optional dot-separated path within it, and a
 * human-readable label are supplied by the caller (typically {@link net.openid.conformance.sequence.ValidateJwksSequence})
 * as environment strings.
 */
public class MapJwksToValidationLocation extends AbstractCondition {

	@Override
	@PreEnvironment(strings = { "jwks_validation_source_key", "jwks_source_label" })
	@PostEnvironment(required = "jwks_to_validate")
	public Environment evaluate(Environment env) {

		String sourceKey = env.getString("jwks_validation_source_key");
		String sourcePath = env.getString("jwks_validation_source_path");
		String label = env.getString("jwks_source_label");

		JsonObject jwks;
		if (sourcePath == null || sourcePath.isEmpty()) {
			jwks = env.getObject(sourceKey);
		} else {
			JsonElement el = env.getElementFromObject(sourceKey, sourcePath);
			jwks = (el != null && el.isJsonObject()) ? el.getAsJsonObject() : null;
		}

		if (jwks == null) {
			throw error("Could not find a JWK set to validate for " + label,
				args("source_key", sourceKey, "source_path", sourcePath == null ? "" : sourcePath));
		}

		env.putObject("jwks_to_validate", jwks);
		logSuccess("Selected the JWK set in " + label + " for validation", args("jwks", jwks));
		return env;
	}
}
