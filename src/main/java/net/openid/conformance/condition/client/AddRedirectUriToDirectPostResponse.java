package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddRedirectUriToDirectPostResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = "direct_post_response")
	@PostEnvironment(required = "direct_post_response")
	public Environment evaluate(Environment env) {

		JsonObject response = env.getObject("direct_post_response");

		response.addProperty("redirect_uri", env.getString("redirect_uri") + "#" + env.getString("code_verifier"));

		logSuccess("Added redirect_uri containing code_verifier in fragment to direct_post_response");

		return env;
	}

}
