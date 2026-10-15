package net.openid.conformance.authzen;

import net.openid.conformance.authzen.condition.CaptureAuthzenSearchResultsForIdempotencyCheck;
import net.openid.conformance.authzen.condition.EnsureAuthzenSearchResultsMatchIdempotencyCheck;

/**
 * Sends the same Action Search request multiple times consecutively, follows every page of each
 * response, and checks that the PDP returns the same results each time. Certification scenario
 * 5.6 requires this of decision values only, so a difference in search results is reported as
 * a warning.
 * The loop body is shared with the other idempotency families via
 * {@link AbstractAuthzenPDPTest#runIdempotencyLoop}.
 */
public abstract class AbstractAuthzenPDPActionSearchIdempotencyTest extends AbstractAuthzenPDPActionSearchTest {

	@Override
	protected void performAuthzenApiFlow() {
		runIdempotencyLoop("Action Search",
			CaptureAuthzenSearchResultsForIdempotencyCheck.class,
			EnsureAuthzenSearchResultsMatchIdempotencyCheck.class);
	}
}
