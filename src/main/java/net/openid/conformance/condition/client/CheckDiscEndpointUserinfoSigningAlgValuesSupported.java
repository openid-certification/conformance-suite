package net.openid.conformance.condition.client;

import java.util.Arrays;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckDiscEndpointUserinfoSigningAlgValuesSupported extends ValidateJsonArray {

	private static final String environmentVariable = "userinfo_signing_alg_values_supported";

	private static final String[] SET_VALUES = new String[] { "PS256", "ES256" };

	private static final String errorMessageNotEnough = "No matching value from server";

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		return validate(env, environmentVariable, Arrays.asList(SET_VALUES), 1,
				errorMessageNotEnough);

	}


}
