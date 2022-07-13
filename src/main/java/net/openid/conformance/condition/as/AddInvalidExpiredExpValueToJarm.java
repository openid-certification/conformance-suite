package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;

public class AddInvalidExpiredExpValueToJarm extends AbstractCondition {

	@Override
	@PreEnvironment(required = "jarm_response_claims")
	@PostEnvironment(required = "jarm_response_claims")
	public Environment evaluate(Environment env) {

		JsonObject claims = env.getObject("jarm_response_claims");

		Instant exp = Instant.now().minusSeconds(60 * 6);

		claims.addProperty("exp", exp.getEpochSecond());

		env.putObject("jarm_response_claims", claims);

		logSuccess("Added expired exp value to JARM claims", args("jarm_response_claims", claims, "exp", exp));

		return env;

	}

}
