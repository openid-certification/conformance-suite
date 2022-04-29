package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.time.Instant;

public class SetDpopIatToOneHourInFuture extends AbstractCondition {

	@Override
	@PreEnvironment(required = "dpop_proof_claims")
	public Environment evaluate(Environment env) {

		JsonObject claims = env.getObject("dpop_proof_claims");

		Instant exp = Instant.now().plusSeconds(60 * 60);

		claims.addProperty("iat", exp.getEpochSecond());

		logSuccess("Set 'iat' in DPoP proof to one hour in future", claims);

		return env;

	}
}
