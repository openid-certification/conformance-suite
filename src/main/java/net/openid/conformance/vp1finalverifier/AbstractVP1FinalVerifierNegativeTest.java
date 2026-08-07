package net.openid.conformance.vp1finalverifier;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs4xx;
import net.openid.conformance.condition.common.ExpectVerifierRejectedPresentationPage;

/**
 * Base class for VP1 Final verifier negative tests. The verifier may either reject the
 * invalid presentation immediately at the response_uri (4xx: the test finishes as PASSED),
 * or return a success response and defer VP verification until after the redirect — which
 * OID4VP 1.0 Final permits, since it does not define "successfully processed" (issue #1828).
 * In the deferred case the tester must upload a screenshot showing the verifier reporting
 * the verification failure, and the test ends as REVIEW.
 */
public abstract class AbstractVP1FinalVerifierNegativeTest extends AbstractVP1FinalVerifierTest {

	@Override
	protected void validateDirectPostEndpointResponse() {
		if (directPostResponseWas2xx()) {
			eventLog.log(getName(), "The verifier returned a success response from the response_uri, "
				+ "i.e. it defers VP verification until after this step, which OID4VP 1.0 Final permits. "
				+ "A screenshot showing the verifier reporting the verification failure must be uploaded "
				+ "for the test to finish.");
			// A success response must still be a well-formed section 8.2 response.
			super.validateDirectPostEndpointResponse();
		} else {
			// Logs success for 4xx (immediate rejection at the response_uri); fails the test
			// with an actionable message for anything else (3xx/5xx/no status).
			callAndContinueOnFailure(EnsureHttpStatusCodeIs4xx.class, ConditionResult.FAILURE, "OID4VP-1FINAL-8.2");
		}
	}

	@Override
	protected void createScreenshotPlaceholder() {
		callAndStopOnFailure(ExpectVerifierRejectedPresentationPage.class, "OID4VP-1FINAL-8.2");
	}
}
