package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Arrays;

public class FAPICheckDiscEndpointRequestObjectEncryptionAlgValuesSupportedContainsRsaOaep extends AbstractValidateJsonArray {

	private static final String environmentVariable = "request_object_encryption_alg_values_supported";

	private static final String[] SET_VALUES = { "RSA-OAEP" };

	private static final String errorMessageNotEnough = "RSA-OAEP support is required but it is not listed in 'request_object_encryption_alg_values_supported'";

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {
		return validate(env, environmentVariable, Arrays.asList(SET_VALUES), 1, errorMessageNotEnough);
	}
}
