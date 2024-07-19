package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;

// This condition is meant to test for 10 seconds but set to 8 to allow for network latency
public class SetDpopIatTo8SecondsInFuture extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"dpop_proof_claims"})
	public Environment evaluate(Environment env) {
		JsonObject claims = env.getObject("dpop_proof_claims");
		Instant iat = Instant.now().plusSeconds(8L);
		claims.addProperty("iat", iat.getEpochSecond());
		logSuccess("Set DPoP proof 'iat' claim to 10 seconds in the future", claims);
		return env;
	}
}
