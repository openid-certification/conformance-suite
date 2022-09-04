package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Arrays;

public class OIDCCCheckDiscEndpointIdTokenSigningAlgValuesSupported extends AbstractValidateJsonArray {

	private static final String environmentVariable = "id_token_signing_alg_values_supported";

	private static final String[] SET_VALUES = { "RS256" };
	private static final int minimumMatchesRequired = SET_VALUES.length;

	private static final String errorMessageNotEnough = "RS256 support is required, but the server does not list it in " + environmentVariable;

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {
		return validate(env, environmentVariable, Arrays.asList(SET_VALUES), minimumMatchesRequired, errorMessageNotEnough);
	}
}
