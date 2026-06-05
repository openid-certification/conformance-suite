package net.openid.conformance.authzen;

import net.openid.conformance.authzen.condition.CaptureAuthzenResponseBodyForIdempotencyCheck;
import net.openid.conformance.authzen.condition.EnsureAuthzenResponseBodyMatchesIdempotencyCheck;

/**
 * Sends the same Evaluations request multiple times consecutively and asserts
 * the PDP returns the same response body each time. The cert profile only defines idempotency for the single
 * Evaluation API (section 2.6); the same principle is exercised here for
 * batch Evaluations API. The loop body is shared with the other idempotency families via
 * {@link AbstractAuthzenPDPTest#runIdempotencyLoop}.
 */
public abstract class AbstractAuthzenPDPEvaluationsIdempotencyTest extends AbstractAuthzenPDPEvaluationsTest {

	@Override
	protected void performAuthzenApiFlow() {
		runIdempotencyLoop("Evaluations",
			CaptureAuthzenResponseBodyForIdempotencyCheck.class,
			EnsureAuthzenResponseBodyMatchesIdempotencyCheck.class);
	}
}
