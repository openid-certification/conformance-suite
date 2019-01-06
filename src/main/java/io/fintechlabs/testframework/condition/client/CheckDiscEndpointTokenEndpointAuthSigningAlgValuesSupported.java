//Author: ddrysdale

package io.fintechlabs.testframework.condition.client;

import java.util.Arrays;

import com.google.gson.JsonElement;

import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class CheckDiscEndpointTokenEndpointAuthSigningAlgValuesSupported extends ValidateJsonArray {

	private static final String environmentVariable = "token_endpoint_auth_signing_alg_values_supported";

	private static final String[] SET_VALUES = new String[] { "PS256", "ES256" };

	private static final String errorMessageNotEnough = "No matching value from server";


	public CheckDiscEndpointTokenEndpointAuthSigningAlgValuesSupported(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		final String[] valuesRequired = new String[] { "private_key_jwt" };
		JsonElement serverValues = env.getElementFromObject("server", "token_endpoint_auth_methods_supported");

		if ( serverValues != null && serverValues.isJsonArray() ) {

			if ( countMatchingElements(Arrays.asList(valuesRequired), serverValues.getAsJsonArray()) > 0 ) {
				return validate(env, environmentVariable, Arrays.asList(SET_VALUES), 1,
						errorMessageNotEnough);
			}
		}

		String logMessage = "Not checking token_endpoint_auth_signing_alg_values_supported as token_endpoint_auth_methods_supported does not contain the method (private_key_jwt ) that requires signing";

		logSuccess(logMessage, args("actual", serverValues, "expected", valuesRequired));
		return env;
	}
}
