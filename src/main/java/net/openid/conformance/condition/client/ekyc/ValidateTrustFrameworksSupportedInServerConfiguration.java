package net.openid.conformance.condition.client.ekyc;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
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
		//TODO require at least one entry? or is an empty value is also allowed?

		logSuccess("trust_frameworks_supported is valid", args("actual", jsonElement));
		return env;
	}
}
