package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;

public class AddNbfToRequestObject extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "request_object_claims"})
	@PostEnvironment(required = { "request_object_claims"})
	public Environment evaluate(Environment env) {

		JsonObject requestObjectClaims = env.getObject("request_object_claims");

		Instant nbf = Instant.now();

		requestObjectClaims.addProperty("nbf", nbf.getEpochSecond());

		env.putObject("request_object_claims", requestObjectClaims);

		logSuccess("Added nbf to request object claims", args(
			"nbf", requestObjectClaims.getAsJsonPrimitive("nbf"))
		);

		return env;
	}
}
