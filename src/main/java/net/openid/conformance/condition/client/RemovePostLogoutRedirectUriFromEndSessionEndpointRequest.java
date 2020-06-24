package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class RemovePostLogoutRedirectUriFromEndSessionEndpointRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "end_session_endpoint_request" )
	@PostEnvironment(required = "end_session_endpoint_request")
	public Environment evaluate(Environment env) {

		JsonObject endSessionEndpointRequest = env.getObject("end_session_endpoint_request");

		endSessionEndpointRequest.remove("post_logout_redirect_uri");

		env.putObject("end_session_endpoint_request", endSessionEndpointRequest);

		logSuccess("Removed post_logout_redirect_uri from end session endpoint request", endSessionEndpointRequest);

		return env;

	}

}
