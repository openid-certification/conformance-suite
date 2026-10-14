package net.openid.conformance.authzen;

import net.openid.conformance.authzen.condition.CaptureAuthzenResponseBodyForIdempotencyCheck;
import net.openid.conformance.authzen.condition.EnsureAuthzenEvaluationsDecisionsMatchIdempotencyCheck;

/**
 * Sends the same Evaluations request multiple times consecutively and asserts
 * the PDP returns the same decisions, in the same order, each time. Certification scenario 5.6 requires the
 * same decision for repeated requests, and scenario 3.1 applies it to every certification level.
 * The loop body is shared with the other idempotency families via
 * {@link AbstractAuthzenPDPTest#runIdempotencyLoop}.
 */
public abstract class AbstractAuthzenPDPEvaluationsIdempotencyTest extends AbstractAuthzenPDPEvaluationsTest {

	@Override
	protected void performAuthzenApiFlow() {
		runIdempotencyLoop("Evaluations",
			CaptureAuthzenResponseBodyForIdempotencyCheck.class,
			EnsureAuthzenEvaluationsDecisionsMatchIdempotencyCheck.class);
	}
}
