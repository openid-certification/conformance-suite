package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Arrays;

public class FAPIBrazilCheckDiscEndpointScopesSupportedForPayments extends AbstractValidateJsonArray {

	private static final String environmentVariable = "scopes_supported";

	private static final String[] SET_VALUES = { "openid", "payments" };
	private static final int minimumMatchesRequired = SET_VALUES.length;

	private static final String errorMessageNotEnough = "The server does not support the required scopes";

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		return validate(env, environmentVariable, Arrays.asList(SET_VALUES), minimumMatchesRequired,
				errorMessageNotEnough);

	}

}
