package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddBadRedirectUriToRequestParameters extends AbstractCondition {
	@Override
	@PreEnvironment(required = { "authorization_endpoint_request"})
	public Environment evaluate(Environment env) {
		JsonObject requestParameters = env.getObject("authorization_endpoint_request");
		requestParameters.remove("redirect_uri");
		requestParameters.addProperty("redirect_uri", "https://junk.io/junk/callback");
		env.putObject("authorization_endpoint_request", requestParameters);
		return env;
	}
}
