package net.openid.conformance.authzen;

import net.openid.conformance.authzen.condition.EnsureAuthzenDecisionResponseValMatchesExpected;
import net.openid.conformance.authzen.condition.EnsureValidDecisionResponse;
import net.openid.conformance.authzen.condition.ExtractAuthzenApiEndpointDecisionResponse;
import net.openid.conformance.authzen.condition.ExtractAuthzenDecisionExpectedResponse;
import net.openid.conformance.authzen.condition.WarnAuthzenDecisionUnknownMembers;
import net.openid.conformance.condition.Condition.ConditionResult;

/**
 * Base class for Evaluations API backward-compatibility tests where the request
 * omits the {@code evaluations} member entirely or sends it as an empty array.
 *
 * <p>Per Section 7.1, such a request "behaves in a backwards-compatible manner" with the
 * single Access Evaluation API, so the PDP returns an Access Evaluation response carrying a
 * top-level {@code decision}. Both the response shape and the decision value (fixture rule 1 —
 * alice/read/record-1 = {@code true}) are validated; members a Decision does not define, such as
 * an {@code evaluations} array, are reported as a warning.
 */
public abstract class AbstractAuthzenPDPEvaluationsBackwardCompatTest extends AbstractAuthzenPDPEvaluationsTest {

	@Override
	protected void processAuthApiEndpointResponse() {
		callAndStopOnFailure(ExtractAuthzenApiEndpointDecisionResponse.class, "AUTHZEN-6.2");
		callAndStopOnFailure(EnsureValidDecisionResponse.class, "AUTHZEN-6.2");
		// The request behaved like the single Access Evaluation request, so the response is an
		// Access Evaluation response; an `evaluations` member is one a Decision does not define.
		callAndContinueOnFailure(WarnAuthzenDecisionUnknownMembers.class, ConditionResult.WARNING, "AUTHZEN-5.5", "AUTHZEN-7.1");
	}

	@Override
	protected void validateAuthApiEndpointResponse() {
		callAndContinueOnFailure(new ExtractAuthzenDecisionExpectedResponse(getExpectedDecisionResponseJson()), ConditionResult.FAILURE);
		// The expected decision comes from the certification fixture (scenario 4.4), not from the
		// Authorization API specification, and the scenario has no requirement link prefix.
		callAndContinueOnFailure(EnsureAuthzenDecisionResponseValMatchesExpected.class, ConditionResult.FAILURE);
	}

	/**
	 * The expected single Access Evaluation response. Both backward-compat cases
	 * send alice/read/record-1, which is fixture rule 1 (decision = {@code true}).
	 */
	protected String getExpectedDecisionResponseJson() {
		return """
			{ "decision": true }
			""";
	}

	@Override
	protected String getExpectedEvaluationsResponseJson() {
		// Unused: this family validates a single Access Evaluation response (see
		// getExpectedDecisionResponseJson()), not an `evaluations` array. The
		// abstract method is satisfied with a harmless placeholder that is never
		// parsed because validateAuthApiEndpointResponse() is overridden above.
		return "{}";
	}
}
