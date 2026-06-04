package net.openid.conformance.authzen;

import net.openid.conformance.authzen.condition.CaptureDecisionForIdempotencyCheck;
import net.openid.conformance.authzen.condition.EnsureDecisionMatchesIdempotencyCheck;
import net.openid.conformance.condition.Condition.ConditionResult;

/**
 * Sends the same Evaluation request multiple times consecutively and asserts the
 * PDP returns the same `decision` value each time (cert profile section 2.6).
 * Subclasses provide only the payload and (optionally) the expected decision.
 */
public abstract class AbstractAuthzenPDPEvaluationIdempotencyTest extends AbstractAuthzenPDPEvaluationTest {

	private static final int ITERATIONS = 3;

	@Override
	protected void performAuthzenApiFlow() {
		eventLog.startBlock("Idempotency test: send the same Evaluation request " + ITERATIONS + " times");

		for (int i = 1; i <= ITERATIONS; i++) {
			eventLog.startBlock("Iteration " + i);
			createAuthzenApiRequest();
			performSingleApiRequest();
			processAuthApiEndpointResponse();
			validateAuthApiEndpointResponse();

			if (i == 1) {
				callAndStopOnFailure(CaptureDecisionForIdempotencyCheck.class, "AUTHZEN-CERT-2.6");
			} else {
				callAndContinueOnFailure(EnsureDecisionMatchesIdempotencyCheck.class, ConditionResult.FAILURE, "AUTHZEN-CERT-2.6");
			}
			eventLog.endBlock();
		}

		performPostApiFlow();
		eventLog.endBlock();
	}
}
