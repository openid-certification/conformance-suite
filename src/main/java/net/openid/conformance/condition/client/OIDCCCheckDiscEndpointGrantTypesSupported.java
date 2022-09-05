package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class OIDCCCheckDiscEndpointGrantTypesSupported extends AbstractCondition {

	private static final String environmentVariable = "grant_types_supported";

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		JsonElement grantTypesSupported = env.getElementFromObject("server", environmentVariable);
		if (grantTypesSupported != null) {
			if (!grantTypesSupported.isJsonArray()) {
				throw error(environmentVariable + " in discovery document, if present, must be an array.", args(environmentVariable, grantTypesSupported));
			}
			JsonArray a = grantTypesSupported.getAsJsonArray();
			if (a.size() == 0) {
				throw error(environmentVariable + " in discovery document must not be an empty array.");
			}
			logSuccess(environmentVariable + " is a non-empty array.", args(environmentVariable, grantTypesSupported));
		} else {
			logSuccess(environmentVariable + " not present in server configuration (so will default to authorization_code and implicit).");
		}

		return env;

	}
}
