package net.openid.conformance.authzen;

import net.openid.conformance.authzen.condition.CaptureAuthzenResponseBodyForIdempotencyCheck;
import net.openid.conformance.authzen.condition.EnsureAuthzenResponseBodyMatchesIdempotencyCheck;
import net.openid.conformance.condition.Condition.ConditionResult;

/**
 * Sends the same Resource Search request multiple times consecutively and asserts
 * the PDP returns the same response body each time.
 */
public abstract class AbstractAuthzenPDPResourceSearchIdempotencyTest extends AbstractAuthzenPDPResourceSearchTest {

	private static final int ITERATIONS = 3;

	@Override
	protected void performAuthzenApiFlow() {
		eventLog.startBlock("Idempotency test: send the same Resource Search request " + ITERATIONS + " times");

		for (int i = 1; i <= ITERATIONS; i++) {
			eventLog.startBlock("Iteration " + i);
			createAuthzenApiRequest();
			performSingleApiRequest();
			processAuthApiEndpointResponse();
			validateAuthApiEndpointResponse();

			if (i == 1) {
				callAndStopOnFailure(CaptureAuthzenResponseBodyForIdempotencyCheck.class, "AUTHZEN-CERT-4.6");
			} else {
				callAndContinueOnFailure(EnsureAuthzenResponseBodyMatchesIdempotencyCheck.class, ConditionResult.FAILURE, "AUTHZEN-CERT-4.6");
			}
			eventLog.endBlock();
		}

		performPostApiFlow();
		eventLog.endBlock();
	}
}
