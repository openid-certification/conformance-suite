package net.openid.conformance.authzen.condition;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Map;
import java.util.Set;

/**
 * Section 5.5: a Decision has a REQUIRED decision and an OPTIONAL context, and nothing else.
 * Reads the Access Evaluation response in {@code authzen_api_endpoint_decision}.
 */
public class WarnAuthzenDecisionUnknownMembers extends AbstractWarnAuthzenUnknownMembers {

	static final Set<String> DECISION_MEMBERS = Set.of("decision", "context");

	@Override
	@PreEnvironment(required = "authzen_api_endpoint_decision")
	public Environment evaluate(Environment env) {
		Map<String, Set<String>> unknown = newUnknownMembers();
		collectUnknownMembers(env.getObject("authzen_api_endpoint_decision"), "$", DECISION_MEMBERS, unknown);
		return reportUnknownMembers(env, unknown, "Access Evaluation response");
	}
}
