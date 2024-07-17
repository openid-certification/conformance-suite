package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;

public class SetDpopIatTo10SecondsInPast extends AbstractCondition {

	@Override

	@PreEnvironment(required = {"dpop_proof_claims"})
	public Environment evaluate(Environment env) {
		JsonObject claims = env.getObject("dpop_proof_claims");
		Instant iat = Instant.now().minusSeconds(10L);
		claims.addProperty("iat", iat.getEpochSecond());
		logSuccess("Set DPoP proof 'iat' claim to 10 seconds in the past", claims);
		return env;
	}

}
