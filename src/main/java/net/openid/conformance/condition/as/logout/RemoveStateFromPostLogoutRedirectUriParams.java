package net.openid.conformance.condition.as.logout;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class RemoveStateFromPostLogoutRedirectUriParams extends AbstractCondition {


	@Override
	@PreEnvironment(required = "post_logout_redirect_uri_params")
	@PostEnvironment(required = "post_logout_redirect_uri_params")
	public Environment evaluate(Environment env) {

		JsonObject responseParams = env.getObject("post_logout_redirect_uri_params");
		if(responseParams.has("state")) {
			responseParams.remove("state");
			log("Removed state from end_session_endpoint response parameters", args("params", responseParams));
			env.putObject("post_logout_redirect_uri_params", responseParams);
		} else {
			log("end_session_endpoint response parameters does not contain a state parameter", args("params", responseParams));
		}

		return env;

	}

}
