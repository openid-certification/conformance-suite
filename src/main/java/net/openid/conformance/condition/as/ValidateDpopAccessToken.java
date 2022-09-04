package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateDpopAccessToken extends AbstractCondition {

	@Override
	@PreEnvironment(strings =  "incoming_dpop_access_token", required = "dpop_access_token")
	public Environment evaluate(Environment env) {

		String dpopAccessToken = env.getString("dpop_access_token", "value");
		String incomingDpopToken = env.getString("incoming_dpop_access_token");
		if(dpopAccessToken.equals(incomingDpopToken)) {
			logSuccess("DPoP Access Token is valid", args("DPoP Access Token", dpopAccessToken));
			return env;
		} else {
			throw error("Invalid DPoP Access Token", args("expected", dpopAccessToken, "actual", incomingDpopToken));
		}
	}

}
