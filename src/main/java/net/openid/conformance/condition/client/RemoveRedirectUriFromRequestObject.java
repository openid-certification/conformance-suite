package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class RemoveRedirectUriFromRequestObject extends AbstractCondition {

	@Override
	@PreEnvironment(required = "request_object_claims")
	public Environment evaluate(Environment env) {

		JsonObject req = env.getObject("request_object_claims");

		req.remove("redirect_uri");

		env.putObject("request_object_claims", req);

		logSuccess("Removed redirect_uri from request object claims", args("request_object_claims", req));

		return env;
	}

}
