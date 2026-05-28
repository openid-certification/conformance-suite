package net.openid.conformance.authzen;

import net.openid.conformance.authzen.condition.CaptureAuthzenResponseBodyForIdempotencyCheck;
import net.openid.conformance.authzen.condition.EnsureAuthzenResponseBodyMatchesIdempotencyCheck;
import net.openid.conformance.condition.Condition.ConditionResult;

/**
 * Sends the same Evaluations request multiple times consecutively and asserts the
 * PDP returns the same response body each time (cert profile section 3.6).
 * Subclasses provide only the payload and expected response.
 */
public abstract class AbstractAuthzenPDPEvaluationsIdempotencyTest extends AbstractAuthzenPDPEvaluationsTest {

	private static final int ITERATIONS = 3;

	@Override
	protected void performAuthzenApiFlow() {
		eventLog.startBlock("Idempotency test: send the same Evaluations request " + ITERATIONS + " times");

		for (int i = 1; i <= ITERATIONS; i++) {
			eventLog.startBlock("Iteration " + i);
			createAuthzenApiRequest();
			callAuthApiEndpointRequest();
			processAuthApiEndpointResponse();
			validateAuthApiEndpointResponse();

			if (i == 1) {
				callAndStopOnFailure(CaptureAuthzenResponseBodyForIdempotencyCheck.class, "AUTHZEN-CERT-3.6");
			} else {
				callAndContinueOnFailure(EnsureAuthzenResponseBodyMatchesIdempotencyCheck.class, ConditionResult.FAILURE, "AUTHZEN-CERT-3.6");
			}
			eventLog.endBlock();
		}

		performPostApiFlow();
		eventLog.endBlock();
	}
}
