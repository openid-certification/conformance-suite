package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

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
