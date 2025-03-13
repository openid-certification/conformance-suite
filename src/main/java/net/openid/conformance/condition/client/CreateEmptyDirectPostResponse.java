package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreateEmptyDirectPostResponse extends AbstractCondition {

	@Override
	@PostEnvironment(required = "direct_post_response")
	public Environment evaluate(Environment env) {

		env.putObject("direct_post_response", new JsonObject());

		logSuccess("Created empty direct_post_response");

		return env;
	}

}
