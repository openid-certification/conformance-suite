package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;
import org.springframework.util.StringUtils;

public abstract class AbstractAddRequestedExpToAuthorizationEndpointRequest extends AbstractCondition {

	protected abstract String getExpectedRequestedExpiry();

	@Override
	@PreEnvironment(required = { "authorization_endpoint_request"} )
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {

		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");

		String requestedExpiry = getExpectedRequestedExpiry();
		if (StringUtils.isEmpty(requestedExpiry)) {
			throw error("requested_expiry missing/empty");
		}

		authorizationEndpointRequest.addProperty("requested_expiry", requestedExpiry);

		env.putObject("authorization_endpoint_request", authorizationEndpointRequest);

		logSuccess("Added requested expiry to authorization endpoint request", args("requested_expiry", requestedExpiry));

		return env;
	}
}
