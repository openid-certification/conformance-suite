package net.openid.conformance.vp1finalwallet;

import net.openid.conformance.condition.client.AddNonceToAuthorizationEndpointRequest;
import net.openid.conformance.condition.common.ExpectRedirectUriErrorPage;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;

@PublishTestModule(
	testName = "oid4vp-1final-wallet-negative-test-missing-nonce",
	displayName = "OID4VP-1.0-FINAL: Authorization request without nonce",
	summary = "Sends an authorization request without a nonce parameter. The nonce is required for key binding JWT "
		+ "verification. The wallet should reject the request and display an error, a screenshot of which must be uploaded.",
	profile = "OID4VP-1FINAL"
)
public class VP1FinalWalletMissingNonce extends AbstractVP1FinalWalletTest {

	@Override
	protected ConditionSequence createAuthorizationRequestSequence() {
		ConditionSequence steps = super.createAuthorizationRequestSequence();

		steps = steps.skip(AddNonceToAuthorizationEndpointRequest.class,
			"Skipping nonce to test wallet rejection of requests without nonce");

		return steps;
	}

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectRedirectUriErrorPage.class, "OID4VP-1FINAL-5");
		env.putString("error_callback_placeholder", env.getString("redirect_uri_error"));
	}

	@Override
	protected void continueAfterRequestUriCalled() {
		eventLog.log(getName(), "Wallet has retrieved request_uri - the request has no nonce, so the wallet should display an error.");
		createPlaceholder();
		waitForPlaceholders();
	}

	@Override
	protected Object handleDirectPost(String requestId) {
		throw new TestFailureException(getId(), "Direct post endpoint was called but the wallet should have rejected the request due to missing nonce");
	}

	@Override
	protected void processBrowserApiResponse() {
		handleBrowserApiResponseAsNegativeTest(
			"Browser API returned a successful response but the wallet should have rejected the request due to missing nonce");
	}
}
