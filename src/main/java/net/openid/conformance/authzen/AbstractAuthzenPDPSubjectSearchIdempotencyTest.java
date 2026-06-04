package net.openid.conformance.authzen;

import net.openid.conformance.authzen.condition.CaptureAuthzenResponseBodyForIdempotencyCheck;
import net.openid.conformance.authzen.condition.EnsureAuthzenResponseBodyMatchesIdempotencyCheck;
import net.openid.conformance.condition.Condition.ConditionResult;

/**
 * Sends the same Subject Search request multiple times consecutively and asserts
 * the PDP returns the same response body each time. The cert profile only
 * defines idempotency for the single Evaluation API (section 2.6); the same
 * principle is exercised here for Subject Search.
 */
public abstract class AbstractAuthzenPDPSubjectSearchIdempotencyTest extends AbstractAuthzenPDPSubjectSearchTest {

	private static final int ITERATIONS = 3;

	@Override
	protected void performAuthzenApiFlow() {
		eventLog.startBlock("Idempotency test: send the same Subject Search request " + ITERATIONS + " times");
		try {
			for (int i = 1; i <= ITERATIONS; i++) {
				eventLog.startBlock("Iteration " + i);
				try {
					createAuthzenApiRequest();
					performSingleApiRequest();
					processAuthApiEndpointResponse();
					validateAuthApiEndpointResponse();

					if (i == 1) {
						callAndStopOnFailure(CaptureAuthzenResponseBodyForIdempotencyCheck.class, "AUTHZEN-CERT-2.6");
					} else {
						callAndContinueOnFailure(EnsureAuthzenResponseBodyMatchesIdempotencyCheck.class, ConditionResult.FAILURE, "AUTHZEN-CERT-2.6");
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
