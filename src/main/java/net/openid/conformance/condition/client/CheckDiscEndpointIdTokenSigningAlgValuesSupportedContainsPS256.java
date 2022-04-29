package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Arrays;

public class CheckDiscEndpointIdTokenSigningAlgValuesSupportedContainsPS256 extends AbstractValidateJsonArray {

	private static final String environmentVariable = "id_token_signing_alg_values_supported";

	private static final String[] SET_VALUES = { "PS256" };

	private static final String errorMessageNotEnough = "Server does not list PS256 in id_token_signing_alg_values_supported";

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {
		return validate(env, environmentVariable, Arrays.asList(SET_VALUES), 1, errorMessageNotEnough);
	}
}
