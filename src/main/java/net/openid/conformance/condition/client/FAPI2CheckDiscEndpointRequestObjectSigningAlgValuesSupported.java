package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Arrays;

public class FAPI2CheckDiscEndpointRequestObjectSigningAlgValuesSupported extends AbstractValidateJsonArray {

	private static final String environmentVariable = "request_object_signing_alg_values_supported";

	private static final String errorMessageNotEnough = "No matching value from server";

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {
		return validate(env,
			environmentVariable,
			Arrays.asList(FAPI2CheckDiscEndpointIdTokenSigningAlgValuesSupported.FAPI2_ALLOWED_ALGS),
			1,
			errorMessageNotEnough);
	}
}
