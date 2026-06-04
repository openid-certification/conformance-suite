package net.openid.conformance.authzen;

import net.openid.conformance.authzen.condition.CaptureAuthzenResponseBodyForIdempotencyCheck;
import net.openid.conformance.authzen.condition.EnsureAuthzenResponseBodyMatchesIdempotencyCheck;
import net.openid.conformance.condition.Condition.ConditionResult;

/**
 * Sends the same Evaluations request multiple times consecutively and asserts the
 * PDP returns the same response body each time. The cert profile only defines
 * idempotency for the single Evaluation API (section 2.6); the same principle is
 * exercised here for the batch Evaluations API.
 * Subclasses provide only the payload and expected response.
 */
public abstract class AbstractAuthzenPDPEvaluationsIdempotencyTest extends AbstractAuthzenPDPEvaluationsTest {

	private static final int ITERATIONS = 3;

	@Override
	protected void performAuthzenApiFlow() {
		eventLog.startBlock("Idempotency test: send the same Evaluations request " + ITERATIONS + " times");
		try {
			for (int i = 1; i <= ITERATIONS; i++) {
				eventLog.startBlock("Iteration " + i);
				try {
					createAuthzenApiRequest();
					performSingleApiRequest();
					processAuthApiEndpointResponse();
					validateAuthApiEndpointResponse();

					if (i == 1) {
						callAndStopOnFailure(CaptureAuthzenResponseBodyForIdempotencyCheck.class);
					} else {
						callAndContinueOnFailure(EnsureAuthzenResponseBodyMatchesIdempotencyCheck.class, ConditionResult.FAILURE);
					}
				} finally {
					eventLog.endBlock();
				}
			}

			performPostApiFlow();
		} finally {
			eventLog.endBlock();
		}
	}
}
