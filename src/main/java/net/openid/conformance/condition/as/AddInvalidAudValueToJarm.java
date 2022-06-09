package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddInvalidAudValueToJarm extends AbstractCondition {

	@Override
	@PreEnvironment(required = "jarm_response_claims")
	@PostEnvironment(required = "jarm_response_claims")
	public Environment evaluate(Environment env) {

		JsonObject claims = env.getObject("jarm_response_claims");

		String aud = env.getString("jarm_response_claims", "aud");

		//Add number 1 onto end of aud string
		if(aud != null) {
			String concat = (aud + 1);
			claims.addProperty("aud", concat);
			env.putObject("jarm_response_claims", claims);
			logSuccess("Added invalid aud to JARM response claims", args("jarm_response_claims", claims, "aud", concat));
		} else {
			throw error("jarm_response_claims does not contain aud", args("jarm_response_claims", claims));
		}
		return env;
	}

}
