package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Arrays;

public class CheckDiscEndpointBackchannelAuthenticationRequestSigningAlgValuesSupported extends AbstractValidateJsonArray {

	private static final String environmentVariable = "backchannel_authentication_request_signing_alg_values_supported";

	private static final String[] SET_VALUES = { "PS256", "ES256" };

	private static final String errorMessageNotEnough = "No matching value from server";

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		return validate(env, environmentVariable, Arrays.asList(SET_VALUES), 1,
			errorMessageNotEnough);

	}
}
