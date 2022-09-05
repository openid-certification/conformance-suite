package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class RemoveRedirectUriFromAuthorizationEndpointRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {
		JsonObject req = env.getObject("authorization_endpoint_request");

		req.remove("redirect_uri");

		env.putObject("authorization_endpoint_request", req);

		return env;
	}

}
