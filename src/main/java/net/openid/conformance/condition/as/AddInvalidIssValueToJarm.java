package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddInvalidIssValueToJarm extends AbstractCondition {

	@Override
	@PreEnvironment(required = "jarm_response_claims")
	@PostEnvironment(required = "jarm_response_claims")
	public Environment evaluate(Environment env) {

		JsonObject claims = env.getObject("jarm_response_claims");

		String iss = env.getString("jarm_response_claims", "iss");

		//Add number 1 onto end of iss string
		if(iss != null) {
			String concat = (iss + 1);
			claims.addProperty("iss", concat);
			env.putObject("jarm_response_claims", claims);
			logSuccess("Added invalid iss to JARM response claims", args("jarm_response_claims", claims, "iss", concat));
		} else {
			throw error("jarm_response_claims does not contain iss", args("jarm_response_claims", claims));
		}
		return env;
	}

}
