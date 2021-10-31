package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.UUID;

public class InjectWrongISSToJWT extends AbstractCondition {
	@Override
	@PreEnvironment(required = { "request_object_claims"})
	@PostEnvironment(required = { "request_object_claims"})
	public Environment evaluate(Environment env) {
		JsonObject requestObjectClaims = env.getObject("request_object_claims");
		log(env.getObject("request_object_claims"));

		final String newISS = UUID.randomUUID().toString();

		requestObjectClaims.addProperty("iss", newISS);

		env.putObject("request_object_claims", requestObjectClaims);

		logSuccess("Added jti to request object claims", args(
			"iss", newISS)
		);

		return env;
	}
}
