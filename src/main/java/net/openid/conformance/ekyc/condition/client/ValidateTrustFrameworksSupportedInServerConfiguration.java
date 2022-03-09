package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateTrustFrameworksSupportedInServerConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {
		JsonElement jsonElement = env.getElementFromObject("server", "trust_frameworks_supported");
		if(jsonElement == null) {
			throw error("trust_frameworks_supported is not set");
		}
		if(!jsonElement.isJsonArray()) {
			throw error("trust_frameworks_supported must be a json array", args("actual", jsonElement));
		}
		//trust_frameworks_supported: REQUIRED. JSON array containing all supported trust frameworks.
		// This array must have at least one member.
		if(jsonElement.getAsJsonArray().size()<1) {
			throw error("trust_frameworks_supported must have at least one member");
		}

		logSuccess("trust_frameworks_supported is valid", args("actual", jsonElement));
		return env;
	}
}
