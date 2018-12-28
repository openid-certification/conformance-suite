package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class SetAuthorizationEndpointRequestResponseTypeToCodeIdtoken extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 * @param optional
	 */
	public SetAuthorizationEndpointRequestResponseTypeToCodeIdtoken(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = "authorization_endpoint_request")
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {
		if (!env.containsObject("authorization_endpoint_request")) {
			throw error("Couldn't find authorization endpoint request");
		}

		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");

		authorizationEndpointRequest.addProperty("response_type", "code id_token");

		env.putObject("authorization_endpoint_request", authorizationEndpointRequest);

		logSuccess("Added response_type parameter to request", authorizationEndpointRequest);

		return env;

	}

}
