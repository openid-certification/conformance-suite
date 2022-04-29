package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Arrays;

public class CheckDiscEndpointSubjectTypesSupportedContainsOnlyPairwise extends AbstractValidateJsonArray {

	private static final String environmentVariable = "subject_types_supported";

	private static final String[] SET_VALUES = { "pairwise" };
	private static final int minimumMatchesRequired = 1;

	private static final String errorMessageNotEnough = environmentVariable + " in discovery document is required to contain only 'pairwise'.";

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {
		return validate(env, environmentVariable, Arrays.asList(SET_VALUES), minimumMatchesRequired, errorMessageNotEnough);
	}
}
