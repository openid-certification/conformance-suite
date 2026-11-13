package net.openid.conformance.authzen.condition;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Reads the Access Evaluation response in {@code authzen_api_endpoint_decision}.
 */
public class WarnAuthzenDecisionNullMembers extends AbstractWarnAuthzenNullMembers {

	@Override
	@PreEnvironment(required = "authzen_api_endpoint_decision")
	public Environment evaluate(Environment env) {
		return reportNullMembers(env, "authzen_api_endpoint_decision", "Access Evaluation response");
	}
}
