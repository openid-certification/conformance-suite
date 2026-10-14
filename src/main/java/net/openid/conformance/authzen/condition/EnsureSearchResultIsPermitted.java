package net.openid.conformance.authzen.condition;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Section 8.1: "any result from a Search API, when subsequently used in an Access Evaluation API
 * call, SHOULD result in a "decision": true response". Reads the Access Evaluation response in
 * {@code authzen_api_endpoint_decision}, whose structure EnsureValidDecisionResponse has checked.
 * Intended to be called at WARNING.
 */
public class EnsureSearchResultIsPermitted extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"authzen_api_endpoint_decision", "authzen_api_endpoint_request"})
	public Environment evaluate(Environment env) {
		JsonElement decision = env.getElementFromObject("authzen_api_endpoint_decision", "decision");
		boolean isBoolean = decision != null && decision.isJsonPrimitive() && decision.getAsJsonPrimitive().isBoolean();
		if (!isBoolean || !OIDFJSON.getBoolean(decision)) {
			throw error("The Access Evaluation API did not permit an access request built from a search result",
				args("request", env.getObject("authzen_api_endpoint_request"), "response", env.getObject("authzen_api_endpoint_decision")));
		}
		logSuccess("The Access Evaluation API permits the access request built from the search result",
			args("request", env.getObject("authzen_api_endpoint_request")));
		return env;
	}
}
