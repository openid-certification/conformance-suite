package net.openid.conformance.authzen.condition;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Reads the Access Evaluations response in {@code authzen_evaluations_endpoint_response}.
 */
public class WarnAuthzenEvaluationsResponseNullMembers extends AbstractWarnAuthzenNullMembers {

	@Override
	@PreEnvironment(required = "authzen_evaluations_endpoint_response")
	public Environment evaluate(Environment env) {
		return reportNullMembers(env, "authzen_evaluations_endpoint_response", "Access Evaluations response");
	}
}
