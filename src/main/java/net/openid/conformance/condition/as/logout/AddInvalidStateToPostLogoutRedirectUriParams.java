package net.openid.conformance.condition.as.logout;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class AddInvalidStateToPostLogoutRedirectUriParams extends AbstractCondition {


	@Override
	@PreEnvironment(required = "post_logout_redirect_uri_params")
	@PostEnvironment(required = "post_logout_redirect_uri_params")
	public Environment evaluate(Environment env) {

		JsonObject responseParams = env.getObject("post_logout_redirect_uri_params");
		if(responseParams.has("state")) {
			responseParams.addProperty("state", OIDFJSON.getString(responseParams.get("state")) + "_INVALID");
			log("Added invalid value for state parameter", args("params", responseParams));
			env.putObject("post_logout_redirect_uri_params", responseParams);
		} else {
			//might be better to throw an error assuming that this condition will be used only when state is required?
			responseParams.addProperty("state", "INVALID");
			log("Added invalid value for state parameter", args("params", responseParams));
			env.putObject("post_logout_redirect_uri_params", responseParams);
		}

		return env;

	}

}
