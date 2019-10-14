package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;

public class AddIatToRequestObject extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "request_object_claims"})
	public Environment evaluate(Environment env) {

		JsonObject requestObjectClaims = env.getObject("request_object_claims");

		Instant iat = Instant.now();

		requestObjectClaims.addProperty("iat", iat.getEpochSecond());

		env.putObject("request_object_claims", requestObjectClaims);

		logSuccess("Added iat to request object claims", args(
			"iat", requestObjectClaims.getAsJsonPrimitive("iat"))
		);

		return env;
	}
}
