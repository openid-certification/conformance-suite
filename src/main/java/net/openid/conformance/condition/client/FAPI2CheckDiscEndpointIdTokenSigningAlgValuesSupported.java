package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Arrays;

@SuppressWarnings("MutablePublicArray")
public class FAPI2CheckDiscEndpointIdTokenSigningAlgValuesSupported extends AbstractValidateJsonArray {

	private static final String environmentVariable = "id_token_signing_alg_values_supported";

	public static final String[] FAPI2_ALLOWED_ALGS = { "PS256", "ES256", "EdDSA" };

	private static final String errorMessageNotEnough = "No matching value from server";

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {
		return validate(env, environmentVariable, Arrays.asList(FAPI2_ALLOWED_ALGS), 1, errorMessageNotEnough);
	}
}
