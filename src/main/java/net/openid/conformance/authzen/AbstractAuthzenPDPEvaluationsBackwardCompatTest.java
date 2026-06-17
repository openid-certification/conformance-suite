package net.openid.conformance.authzen;

import net.openid.conformance.authzen.condition.EnsureAuthzenDecisionResponseValMatchesExpected;
import net.openid.conformance.authzen.condition.EnsureBackwardCompatResponseHasNoEvaluationsArray;
import net.openid.conformance.authzen.condition.EnsureValidDecisionResponse;
import net.openid.conformance.authzen.condition.ExtractAuthzenApiEndpointDecisionResponse;
import net.openid.conformance.authzen.condition.ExtractAuthzenDecisionExpectedResponse;
import net.openid.conformance.condition.Condition.ConditionResult;

/**
 * Base class for Evaluations API backward-compatibility tests where the request
 * omits the {@code evaluations} member entirely or sends it as an empty array.
 *
 * <p>Per Section 7.1,  such a request MUST be handled in a backwards-compatible
 * manner with the single Access Evaluation API: the PDP returns a single Access
 * Evaluation response carrying a top-level {@code decision} and MUST NOT return
 * an {@code evaluations} array. Both the response shape and the decision value
 * (fixture rule 1 — alice/read/record-1 = {@code true}) are validated.
 */
public abstract class AbstractAuthzenPDPEvaluationsBackwardCompatTest extends AbstractAuthzenPDPEvaluationsTest {

	@Override
	protected void processAuthApiEndpointResponse() {
		callAndStopOnFailure(ExtractAuthzenApiEndpointDecisionResponse.class, "AUTHZEN-6.2");
		// The request behaved like the single Access Evaluation request, so the
		// response must be the single-decision form — no `evaluations` array.
		callAndContinueOnFailure(EnsureBackwardCompatResponseHasNoEvaluationsArray.class, ConditionResult.FAILURE, "AUTHZEN-7.1");
		callAndStopOnFailure(EnsureValidDecisionResponse.class, "AUTHZEN-6.2");
	}

	@Override
	protected void validateAuthApiEndpointResponse() {
		callAndContinueOnFailure(new ExtractAuthzenDecisionExpectedResponse(getExpectedDecisionResponseJson()), ConditionResult.FAILURE);
		callAndContinueOnFailure(EnsureAuthzenDecisionResponseValMatchesExpected.class, ConditionResult.FAILURE, "AUTHZEN-6.2");
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
