package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class AddPostLogoutRedirectUriWithQueryAddedToEndSessionEndpointRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "end_session_endpoint_request")
	@PostEnvironment(required = "end_session_endpoint_request")
	public Environment evaluate(Environment env) {

		JsonObject endSessionEndpointRequest = env.getObject("end_session_endpoint_request");

		String postLogoutUri = OIDFJSON.getString(endSessionEndpointRequest.get("post_logout_redirect_uri"));

		postLogoutUri += "?foo=bar";

		endSessionEndpointRequest.addProperty("post_logout_redirect_uri", postLogoutUri);

		env.putObject("end_session_endpoint_request", endSessionEndpointRequest);

		logSuccess("Added ?foo=bar to post_logout_redirect_uri in end session endpoint request", endSessionEndpointRequest);

		return env;

	}

}
