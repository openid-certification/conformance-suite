package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public abstract class AbstractAddRequestedExpToAuthorizationEndpointRequest extends AbstractCondition {

	public AbstractAddRequestedExpToAuthorizationEndpointRequest(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	protected abstract Integer getExpectedRequestedExpiry();

	@Override
	@PreEnvironment(required = { "authorization_endpoint_request"} )
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {

		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");

		Integer requestedExpiry = getExpectedRequestedExpiry();
		if (requestedExpiry == null || requestedExpiry.intValue() == 0) {
			throw error("requested_expiry missing/empty");
		}

		authorizationEndpointRequest.addProperty("requested_expiry", requestedExpiry);

		env.putObject("authorization_endpoint_request", authorizationEndpointRequest);

		logSuccess("Added requested expiry to authorization endpoint request", args("requested_expiry", requestedExpiry));

		return env;
	}
}
