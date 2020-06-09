package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreateEndSessionEndpointRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "id_token", strings = { "post_logout_redirect_uri" } )
	@PostEnvironment(required = "end_session_endpoint_request")
	public Environment evaluate(Environment env) {

		String idToken = env.getString("id_token", "value");
		if (Strings.isNullOrEmpty(idToken)) {
			throw error("Couldn't find id_token");
		}

		String postLogoutRedirectUri = env.getString("post_logout_redirect_uri");
		if (Strings.isNullOrEmpty(postLogoutRedirectUri)) {
			throw error("Couldn't find post_logout_redirect_uri");
		}

		String state = env.getString("end_session_state");
		if (Strings.isNullOrEmpty(postLogoutRedirectUri)) {
			throw error("Couldn't find end_session_state");
		}

		JsonObject endSessionEndpointRequest = new JsonObject();

		endSessionEndpointRequest.addProperty("id_token_hint", idToken);
		endSessionEndpointRequest.addProperty("post_logout_redirect_uri", postLogoutRedirectUri);
		endSessionEndpointRequest.addProperty("state", state);

		env.putObject("end_session_endpoint_request", endSessionEndpointRequest);

		logSuccess("Created end session endpoint request", endSessionEndpointRequest);

		return env;

	}

}
