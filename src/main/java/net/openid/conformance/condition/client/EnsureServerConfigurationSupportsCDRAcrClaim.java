package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Arrays;

public class EnsureServerConfigurationSupportsCDRAcrClaim extends AbstractValidateJsonArray {

	private static final String environmentVariable = "acr_values_supported";

	private static final String[] SET_VALUES = { "urn:cds.au:cdr:2" };
	private static final int minimumMatchesRequired = 1;

	private static final String errorMessageNotEnough = "The server does not list 'urn:cds.au:cdr:2' in 'acr_values_supported'.";

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		return validate(env, environmentVariable, Arrays.asList(SET_VALUES), minimumMatchesRequired,
				errorMessageNotEnough);

	}

}
