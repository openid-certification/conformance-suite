package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;

public class SetDpopNbfToNow extends AbstractCondition {

	@Override
	@PreEnvironment(required = "dpop_proof_claims")
	public Environment evaluate(Environment env) {

		JsonObject claims = env.getObject("dpop_proof_claims");

		Instant now = Instant.now();

		claims.addProperty("nbf", now.getEpochSecond());

		logSuccess("Set 'nbf' in DPoP proof to now", claims);

		return env;

	}
}
