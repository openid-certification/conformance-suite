package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import com.google.gson.JsonElement;
import java.util.Arrays;

public class EnsureServerConfigurationCodeChallengeMethodsSupportedIsAnArray extends AbstractValidateJsonArray {

	private static final String environmentVariable = "code_challenge_methods_supported";

	private static final String[] SET_VALUES = { "S256", "plain" };

	private static final String errorMessageNotEnough = "No matching value from server";

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		JsonElement serverValues = env.getElementFromObject("server", environmentVariable);

		// For OIDC there is no requirement to support PKCE. Thus a missing claim or a claim
		// with an empty array value is valid.
		if (serverValues == null) {
			logSuccess(environmentVariable + " is not present in the discovery document");
			return env;
		}
		else if (serverValues.isJsonArray() && serverValues.getAsJsonArray().isEmpty()) {
			logSuccess(environmentVariable + " is an empty array");
			return env;
		}

		// Ensure the value as an array and contains a valid entry.
		return validate(env, environmentVariable, Arrays.asList(SET_VALUES), 1, errorMessageNotEnough);
	}
}
