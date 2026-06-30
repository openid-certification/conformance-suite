package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetSubToWrongValueInClientAssertionClaims extends AbstractCondition {

	@Override
	@PreEnvironment(required = "client_assertion_claims")
	@PostEnvironment(required = "client_assertion_claims")
	public Environment evaluate(Environment env) {

		JsonObject claims = env.getObject("client_assertion_claims");

		claims.addProperty("sub", "wrong-sub-value");

		env.putObject("client_assertion_claims", claims);

		logSuccess("Set wrong sub in client_assertion_claims", claims);

		return env;
	}
}
