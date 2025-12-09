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

		String pdpDecisionPoint = env.getString("config", "pdp.policy_decision_point");
		String accessEvaluationEndpoint = env.getString("config", "pdp.access_evaluation_endpoint");

		if (Strings.isNullOrEmpty(pdpDecisionPoint) || Strings.isNullOrEmpty(accessEvaluationEndpoint))  {
			throw error("Test set to use static server configuration but test configuration is missing policy_decision_point or access_evaluation_endpoint", args("policy_decision_point", pdpDecisionPoint, "access_evaluation_endpoint", accessEvaluationEndpoint));
		}

		// make sure we've got a server object
		JsonElement pdp = env.getElementFromObject("config", "pdp");
		if (pdp == null || !pdp.isJsonObject()) {
			throw error("Couldn't find pdp object in configuration");
		} else {
			// we've got a server object, put it in the environment
			env.putObject("pdp", pdp.getAsJsonObject());

			logSuccess("Found a static pdp object", pdp.getAsJsonObject());
			return env;
		}
	}

}
