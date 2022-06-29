package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;

public class SetDpopExpToFiveMinutesInFuture extends AbstractCondition {

	@Override
	@PreEnvironment(required = "dpop_proof_claims")
	public Environment evaluate(Environment env) {

		JsonObject claims = env.getObject("dpop_proof_claims");

		Instant exp = Instant.now().plusSeconds(5 * 60);

		claims.addProperty("exp", exp.getEpochSecond());

		logSuccess("Set 'exp' in DPoP proof to 5 minutes in the future", claims);

		return env;

	}
}
