package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Arrays;

public class CheckDiscEndpointRequestObjectSigningAlgValuesSupportedContainsRS256 extends AbstractValidateJsonArray {

	private static final String ENVIRONMENT_VARIABLE = "request_object_signing_alg_values_supported";

	private static final String[] SET_VALUES = { "RS256" };

	private static final String ERROR_MESSAGE_NOT_ENOUGH = "Discovery endpoint request_object_signing_alg_values_supported does not include RS256.";

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {
		return validate(env, ENVIRONMENT_VARIABLE, Arrays.asList(SET_VALUES), 1, ERROR_MESSAGE_NOT_ENOUGH);
	}
}
