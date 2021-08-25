package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Arrays;

public class OIDCCCheckDiscEndpointResponseTypesSupportedDynamic extends AbstractValidateResponseTypesArray {

	private static final String environmentVariable = "response_types_supported";

	private static final String[] SET_VALUES = {"code", "id_token", "token id_token"}; // from OIDCD-3
	private static final int minimumMatchesRequired = SET_VALUES.length;

	private static final String errorMessageNotEnough = "The server does not support all of the mandatory to implement response_types for dynamic OpenID Providers.";

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		return validate(env, environmentVariable, Arrays.asList(SET_VALUES), minimumMatchesRequired, errorMessageNotEnough);
	}
}
