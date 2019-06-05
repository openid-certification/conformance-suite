package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;
import org.apache.commons.lang3.RandomStringUtils;

public class AddJtiToRequestObject extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "request_object_claims"})
	@PostEnvironment(required = { "request_object_claims"})
	public Environment evaluate(Environment env) {
		JsonObject requestObjectClaims = env.getObject("request_object_claims");

		// RFC 7519 doesn't contain any restrictions on jti other than it "is a case-sensitive string",
		// so this is a very conservative choice of value
		final String jti = RandomStringUtils.randomAlphanumeric(20);
		requestObjectClaims.addProperty("jti", jti);

		env.putObject("request_object_claims", requestObjectClaims);

		logSuccess("Added jti to request object claims", args(
			"jti", jti)
		);

		return env;
	}
}
