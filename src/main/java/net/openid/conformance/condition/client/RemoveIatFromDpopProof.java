package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;

public class RemoveIatFromDpopProof extends AbstractCondition {

	@Override
	@PreEnvironment(required = "dpop_proof_claims")
	public Environment evaluate(Environment env) {

		JsonObject claims = env.getObject("dpop_proof_claims");

		claims.remove("iat");

		logSuccess("Removed 'iat' from DPoP proof", claims);

		return env;

	}
}
