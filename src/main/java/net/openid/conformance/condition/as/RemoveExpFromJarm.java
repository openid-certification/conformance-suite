package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class RemoveExpFromJarm extends AbstractCondition {

	@Override
	@PreEnvironment(required = "jarm_response_claims")
	@PostEnvironment(required = "jarm_response_claims")
	public Environment evaluate(Environment env) {

		JsonObject claims = env.getObject("jarm_response_claims");

		claims.remove("exp");

		env.putObject("jarm_response_claims", claims);

		logSuccess("Removed exp value from JARM claims", args("jarm_response_claims", claims));

		return env;

	}

}
