package net.openid.conformance.authzen;

import net.openid.conformance.authzen.condition.CaptureDecisionForIdempotencyCheck;
import net.openid.conformance.authzen.condition.EnsureDecisionMatchesIdempotencyCheck;

/**
 * Sends the same Evaluation request multiple times consecutively and asserts
 * the PDP returns the same `decision` value each time. The cert profile only defines idempotency for the single
 * Evaluation API (section 2.6); the same principle is exercised here for
 * single Evaluation API. The loop body is shared with the other idempotency families via
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
