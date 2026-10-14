package net.openid.conformance.authzen;

import net.openid.conformance.authzen.condition.CaptureDecisionForIdempotencyCheck;
import net.openid.conformance.authzen.condition.EnsureDecisionMatchesIdempotencyCheck;

/**
 * Sends the same Evaluation request multiple times consecutively and asserts
 * the PDP returns the same `decision` value each time. Certification scenario 5.6 requires the
 * same decision for repeated requests, and scenario 3.1 applies it to every certification level.
 * The loop body is shared with the other idempotency families via
 * {@link AbstractAuthzenPDPTest#runIdempotencyLoop}.
 */
public abstract class AbstractAuthzenPDPEvaluationIdempotencyTest extends AbstractAuthzenPDPEvaluationTest {

	@Override
	protected void performAuthzenApiFlow() {
		runIdempotencyLoop("Evaluation",
			CaptureDecisionForIdempotencyCheck.class,
			EnsureDecisionMatchesIdempotencyCheck.class);
	}
}
