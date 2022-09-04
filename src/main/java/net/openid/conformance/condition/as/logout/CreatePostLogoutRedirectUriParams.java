package net.openid.conformance.condition.as.logout;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreatePostLogoutRedirectUriParams extends AbstractCondition {


	@Override
	@PreEnvironment(required = "end_session_endpoint_http_request_params")
	@PostEnvironment(required = "post_logout_redirect_uri_params")
	public Environment evaluate(Environment env) {

		String state = env.getString("end_session_endpoint_http_request_params", "state");

		JsonObject responseParams = new JsonObject();
		if(state!=null) {
			responseParams.addProperty("state", state);
		}

		log("Added post_logout_redirect_uri parameters to environment", args("params", responseParams));

		env.putObject("post_logout_redirect_uri_params", responseParams);

		return env;

	}

}
