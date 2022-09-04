package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Arrays;

public class OIDCCCheckDiscEndpointUserinfoSigningAlgValuesSupported extends AbstractValidateJsonArray {

	private static final String environmentVariable = "userinfo_signing_alg_values_supported";

	private static final String[] SET_VALUES = {};
	private static final int minimumMatchesRequired = SET_VALUES.length;

	// There are no required values in this case ("none" MAY be included), so the "not enough"
	// message will never be used. We do make use of the checks for being an array.
	private static final String errorMessageNotEnough = null;

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		return validate(env, environmentVariable, Arrays.asList(SET_VALUES), minimumMatchesRequired, errorMessageNotEnough);
	}


}
