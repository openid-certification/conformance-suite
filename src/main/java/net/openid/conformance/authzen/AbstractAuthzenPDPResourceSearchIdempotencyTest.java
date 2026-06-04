package net.openid.conformance.authzen;

import net.openid.conformance.authzen.condition.CaptureAuthzenResponseBodyForIdempotencyCheck;
import net.openid.conformance.authzen.condition.EnsureAuthzenResponseBodyMatchesIdempotencyCheck;

/**
 * Sends the same Resource Search request multiple times consecutively and asserts
 * the PDP returns the same response body each time. The cert profile only defines idempotency for the single
 * Evaluation API (section 2.6); the same principle is exercised here for
 * Resource Search. The loop body is shared with the other idempotency families via
 * {@link AbstractAuthzenPDPTest#runIdempotencyLoop}.
 */
public abstract class AbstractAuthzenPDPResourceSearchIdempotencyTest extends AbstractAuthzenPDPResourceSearchTest {

	@Override
	protected void performAuthzenApiFlow() {
		runIdempotencyLoop("Resource Search",
			CaptureAuthzenResponseBodyForIdempotencyCheck.class,
			EnsureAuthzenResponseBodyMatchesIdempotencyCheck.class);
	}
}
