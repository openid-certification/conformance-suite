package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;

public class AddExpiredExpToRequestObject extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "request_object_claims"})
	public Environment evaluate(Environment env) {

		JsonObject requestObjectClaims = env.getObject("request_object_claims");

		Instant exp = Instant.now().minusSeconds(3600);

		requestObjectClaims.addProperty("exp", exp.getEpochSecond());

		env.putObject("request_object_claims", requestObjectClaims);

		logSuccess("Added expired exp to request object claims", args(
			"exp", requestObjectClaims.getAsJsonPrimitive("exp"))
		);

		return env;
	}
}
