package net.openid.conformance.authzen.condition;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class GetPDPStaticServerConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config")
	@PostEnvironment(required = "pdp")
	public Environment evaluate(Environment env) {

		// make sure we've got a server object
		JsonElement pdp = env.getElementFromObject("config", "pdp");
		if (pdp == null || !pdp.isJsonObject()) {
			throw error("The 'AuthZEN' section is missing from the test configuration");
		}
		String pdpDecisionPoint = env.getString("config", "pdp.policy_decision_point");
		if (Strings.isNullOrEmpty(pdpDecisionPoint)) {
			throw error("'PDP Identifier' field is missing from the 'AuthZEN' section in the test configuration");
		}
		env.putObject("pdp", pdp.getAsJsonObject());
		logSuccess("Found a static pdp object", pdp.getAsJsonObject());
		return env;
	}

}
