package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Map;
import java.util.Set;

/**
 * Section 7.2: the Access Evaluations response is a Decision (Section 5.5) with an added
 * evaluations array, each element of which is a Decision. Reads
 * {@code authzen_evaluations_endpoint_response}.
 */
public class WarnAuthzenEvaluationsResponseUnknownMembers extends AbstractWarnAuthzenUnknownMembers {

	private static final Set<String> RESPONSE_MEMBERS = Set.of("evaluations", "decision", "context");

	@Override
	@PreEnvironment(required = "authzen_evaluations_endpoint_response")
	public Environment evaluate(Environment env) {
		JsonObject response = env.getObject("authzen_evaluations_endpoint_response");
		Map<String, Set<String>> unknown = newUnknownMembers();
		collectUnknownMembers(response, "$", RESPONSE_MEMBERS, unknown);
		collectUnknownMembersOfEach(response.get("evaluations"), "$.evaluations",
			WarnAuthzenDecisionUnknownMembers.DECISION_MEMBERS, unknown);
		return reportUnknownMembers(env, unknown, "Access Evaluations response");
	}
}
