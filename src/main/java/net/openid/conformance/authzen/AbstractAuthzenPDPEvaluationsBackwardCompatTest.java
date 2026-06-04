package net.openid.conformance.authzen;

import net.openid.conformance.authzen.condition.EnsureEvaluationsResponseLengthMatchesRequest;
import net.openid.conformance.authzen.condition.EnsureNoTopLevelDecisionWhenEvaluationsPresent;
import net.openid.conformance.authzen.condition.EnsureValidEvaluationsResponse;
import net.openid.conformance.authzen.condition.ExtractAuthzenApiEndpointEvaluationsResponse;
import net.openid.conformance.authzen.condition.NormalizeAuthzenEvaluationsResponseSingleDecisionToArray;
import net.openid.conformance.condition.Condition.ConditionResult;

/**
 * Base class for Evaluations API backward-compatibility tests where the request
 * omits the `evaluations` member entirely or sends it as an empty array. Per
 * spec 7.1-2 the PDP MAY return either the single-decision form or the
 * one-element evaluations array form; this class inserts a normalization step
 * after the response is extracted so downstream validators only have to handle
 * the array form.
 */
public abstract class AbstractAuthzenPDPEvaluationsBackwardCompatTest extends AbstractAuthzenPDPEvaluationsTest {

	@Override
	protected void processAuthApiEndpointResponse() {
		callAndStopOnFailure(ExtractAuthzenApiEndpointEvaluationsResponse.class, "AUTHZEN-7.2");
		// §7.2-3 SHOULD-omit `decision` when `evaluations` is present — must run
		// BEFORE NormalizeAuthzenEvaluationsResponseSingleDecisionToArray, which
		// rebuilds the response with only `evaluations` and would erase the
		// original top-level `decision` we are trying to detect.
		callAndContinueOnFailure(EnsureNoTopLevelDecisionWhenEvaluationsPresent.class, ConditionResult.WARNING, "AUTHZEN-7.2");
		callAndStopOnFailure(NormalizeAuthzenEvaluationsResponseSingleDecisionToArray.class, "AUTHZEN-7.1");
		callAndStopOnFailure(EnsureValidEvaluationsResponse.class, "AUTHZEN-7.2");
		callAndContinueOnFailure(EnsureEvaluationsResponseLengthMatchesRequest.class, ConditionResult.FAILURE, "AUTHZEN-7.2");
	}
}
