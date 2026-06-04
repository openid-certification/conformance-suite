package net.openid.conformance.authzen.condition;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Section 9.2.3 — the `policy_decision_point` value returned in the metadata
 * document MUST be identical to the URL used to construct the discovery URL.
 * Mismatch means the metadata MUST NOT be used (9.2.3).
 */
public class EnsurePolicyDecisionPointMatchesIssuer extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"config", "server"})
	public Environment evaluate(Environment env) {
		String configured = env.getString("config", "pdp.policy_decision_point");
		if (configured == null) {
			throw error("'Policy Decision Point Identifier' field is missing from the 'PDP' section in the test configuration");
		}
		String returned = env.getString("server", "policy_decision_point");
		if (returned == null) {
			throw error("Discovery document does not contain policy_decision_point");
		}
		if (!configured.equals(returned)) {
			throw error("Discovery document policy_decision_point does not match the URL used to discover it",
				args("configured", configured, "returned", returned));
		}
		logSuccess("Discovery document policy_decision_point matches the discovery URL",
			args("policy_decision_point", returned));
		return env;
	}
}
