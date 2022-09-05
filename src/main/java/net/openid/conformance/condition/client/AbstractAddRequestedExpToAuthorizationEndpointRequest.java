package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public abstract class AbstractAddRequestedExpToAuthorizationEndpointRequest extends AbstractCondition {

	protected abstract Integer getExpectedRequestedExpiry();

	@Override
	@PreEnvironment(required = { "authorization_endpoint_request"} )
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {

		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");

		Integer requestedExpiry = getExpectedRequestedExpiry();
		if (requestedExpiry == null) {
			throw error("requested_expiry missing/empty");
		}

		authorizationEndpointRequest.addProperty("requested_expiry", requestedExpiry);

		env.putObject("authorization_endpoint_request", authorizationEndpointRequest);

		logSuccess("Added requested expiry to authorization endpoint request", args("requested_expiry", requestedExpiry));

		return env;
	}
}
