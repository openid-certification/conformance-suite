package net.openid.conformance.vp;

import net.openid.conformance.condition.client.BuildRequestObjectByReferenceRedirectToAuthorizationEndpointWithoutDuplicates;
import net.openid.conformance.condition.client.InvalidateRequestObjectSignature;
import net.openid.conformance.condition.common.ExpectRedirectUriErrorPage;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.variant.VPRequestMethod;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "oid4vp-negative-test-invalid-request-object-signature",
	displayName = "OID4VP: Request object signature not valid",
	summary = "Makes a request where the signature on the request object JWS is invalid. The wallet should display an error, a screenshot of which must be uploaded.",
	profile = "OID4VP-ID2",
	configurationFields = {
		"client.presentation_definition"
	}
)
@VariantNotApplicable(parameter = VPRequestMethod.class, values={"request_uri_unsigned"})
public class VPID2InvalidRequestObjectSignature extends AbstractVPServerTest {
	@Override
	protected ConditionSequence createAuthorizationRedirectStepsSignedRequestUri() {
		ConditionSequence seq = super.createAuthorizationRedirectStepsSignedRequestUri();

		seq = seq.insertBefore(BuildRequestObjectByReferenceRedirectToAuthorizationEndpointWithoutDuplicates.class,
			condition(InvalidateRequestObjectSignature.class));

		return seq;
	}

	@Override
	protected void createPlaceholder() {
		// FIXME use a better placeholder with a better message
		callAndStopOnFailure(ExpectRedirectUriErrorPage.class, "OID4VP-6.2");

		env.putString("error_callback_placeholder", env.getString("redirect_uri_error"));
	}

	@Override
	protected void continueAfterRequestUriCalled() {
		eventLog.log(getName(), "Wallet has retrieved request_uri - the request object signature is invalid, so the wallet should display an error, a screenshot of which must be uploaded for the test to transition to 'FINISHED'.");
		createPlaceholder();
		waitForPlaceholders();
	}

	@Override
	protected Object handleDirectPost(String requestId) {
		throw new TestFailureException(getId(), "Direct post (response_uri) endpoint has been called but the wallet should have stopped because of the invalid signature on the request object.");
	}

}
