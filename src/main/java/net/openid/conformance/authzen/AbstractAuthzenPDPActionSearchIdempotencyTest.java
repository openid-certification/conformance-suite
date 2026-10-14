package net.openid.conformance.authzen;

import net.openid.conformance.authzen.condition.CaptureAuthzenResponseBodyForIdempotencyCheck;
import net.openid.conformance.authzen.condition.EnsureAuthzenSearchResultsMatchIdempotencyCheck;

/**
 * Sends the same Action Search request multiple times consecutively and asserts
 * the PDP returns the same results each time. Certification scenario 5.6 states this
 * for decision values; the same principle is applied here to Action Search results.
 * The loop body is shared with the other idempotency families via
 * {@link AbstractAuthzenPDPTest#runIdempotencyLoop}.
 */
public abstract class AbstractAuthzenPDPActionSearchIdempotencyTest extends AbstractAuthzenPDPActionSearchTest {

	@Override
	protected void performAuthzenApiFlow() {
		runIdempotencyLoop("Action Search",
			CaptureAuthzenResponseBodyForIdempotencyCheck.class,
			EnsureAuthzenSearchResultsMatchIdempotencyCheck.class);
	}
}
