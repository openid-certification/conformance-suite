package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Arrays;

public class OIDCCCheckDiscEndpointResponseTypesSupported extends AbstractValidateResponseTypesArray {

	private static final String environmentVariable = "response_types_supported";

	private static final String[] SET_VALUES = {"code", "code id_token", "id_token", "token id_token", "code id_token token", "code token"};
	private static final int minimumMatchesRequired = 1;

	private static final String errorMessageNotEnough = "The server must support at least one of the response types defined in OpenID Connect.";

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		return validate(env, environmentVariable, Arrays.asList(SET_VALUES), minimumMatchesRequired, errorMessageNotEnough);
	}
}
