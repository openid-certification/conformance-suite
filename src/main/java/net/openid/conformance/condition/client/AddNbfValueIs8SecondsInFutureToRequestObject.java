package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;

public class AddNbfValueIs8SecondsInFutureToRequestObject extends AbstractCondition {

	@Override
	@PreEnvironment(required = "request_object_claims")
	@PostEnvironment(required = "request_object_claims")
	public Environment evaluate(Environment env) {

		JsonObject requestObjectClaims = env.getObject("request_object_claims");

		Instant nbf = Instant.now().plusSeconds(8);

		requestObjectClaims.addProperty("nbf", nbf.getEpochSecond());

		env.putObject("request_object_claims", requestObjectClaims);

		logSuccess("Added nbf value to request object which is 8 seconds in the future", args("request_object_claims", requestObjectClaims, "nbf_is_8_seconds_in_the_future", nbf));

		return env;

	}

}
