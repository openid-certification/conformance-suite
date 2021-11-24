package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateEvidenceSupportedInServerConfiguration extends AbstractCondition {

	//evidence_supported: REQUIRED. JSON array containing all types of identity evidence the OP uses.
	// This array may have zero or more members.
	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {
		JsonElement jsonElement = env.getElementFromObject("server", "evidence_supported");
		if(jsonElement == null) {
			throw error("evidence_supported is not set in server metadata and is required by the identity assurance specification.");
		}
		if(!jsonElement.isJsonArray()) {
			throw error("evidence_supported must be a json array", args("actual", jsonElement));
		}
		for (JsonElement el: jsonElement.getAsJsonArray()) {
			if (!el.isJsonPrimitive() || !el.getAsJsonPrimitive().isString()) {
				throw error("The entries in evidence_supported must be JSON strings.", args("actual", jsonElement));
			}
		}

		logSuccess("evidence_supported is valid", args("actual", jsonElement));
		return env;
	}
}
