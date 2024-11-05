package net.openid.conformance.openid.federation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateAuthorityHints extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "federation_response_jwt" } )
	public Environment evaluate(Environment env) {

		JsonElement authorityHintsElement = env.getElementFromObject("federation_response_jwt", "claims.authority_hints");
		if (authorityHintsElement == null) {
			throw error("authority_hints is required in entity configurations of entities that have at least one superior",
					args("authority_hints", authorityHintsElement));
		}

		if (!authorityHintsElement.isJsonArray()) {
			throw error("authority_hints must be an array of strings", args("authority_hints", authorityHintsElement));
		}

		JsonArray authorityHints = authorityHintsElement.getAsJsonArray();
		if (authorityHints.isEmpty()) {
			throw error("authority_hints must be non-empty", args("authority_hints", authorityHints));
		}

		logSuccess("Entity statement contains authority_hints", args("authority_hints", authorityHints));
		return env;
	}
}
