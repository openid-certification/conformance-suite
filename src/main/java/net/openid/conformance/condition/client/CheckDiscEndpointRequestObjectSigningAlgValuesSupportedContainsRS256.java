package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Arrays;

public class CheckDiscEndpointRequestObjectSigningAlgValuesSupportedContainsRS256 extends ValidateJsonArray {

	private static final String environmentVariable = "request_object_signing_alg_values_supported";

	private static final String[] SET_VALUES = new String[] { "RS256" };

	private static final String errorMessageNotEnough = "Discovery endpoint request_object_signing_alg_values_supported does not include RS256.";

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {
		return validate(env, environmentVariable, Arrays.asList(SET_VALUES), 1, errorMessageNotEnough);
	}
}
