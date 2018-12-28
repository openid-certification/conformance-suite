//Author: ddrysdale

package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

// author: ddrysdale

public class CheckDiscEndpointJwksUri extends ValidateJsonUri {

	private static final String environmentVariable = "jwks_uri";
	private static final String requiredHostName = "keystore.openbanking.org.uk";

	public CheckDiscEndpointJwksUri(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {
		return validateWithHost(env, environmentVariable, requiredHostName);
	}
}
