package net.openid.conformance.vp1finalwallet;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.AddNonceToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.EnsureErrorFromAuthorizationEndpointResponse;
import net.openid.conformance.condition.client.EnsureInvalidRequestError;
import net.openid.conformance.condition.client.EnsureNoVpTokenInAuthorizationEndpointResponse;
import net.openid.conformance.condition.common.ExpectRedirectUriErrorPage;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "oid4vp-1final-wallet-negative-test-missing-nonce",
	displayName = "OID4VP-1.0-FINAL: Authorization request without nonce",
	summary = """
		Sends an authorization request without a nonce parameter. The nonce is required for key binding JWT \
		verification. The wallet should either return an error response (normally 'invalid_request', as defined \
		by RFC6749 for a missing required parameter), or reject the request and display an error, a screenshot of \
		which must be uploaded.""",
	profile = "OID4VP-1FINAL"
)
public class VP1FinalWalletMissingNonce extends AbstractVP1FinalWalletNegativeTestExpectingError {

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
		eventLog.log(getName(), "Wallet has retrieved request_uri - the request has no nonce, so the wallet should return an error response or display an error.");
		createPlaceholder();
		waitForPlaceholders();
	}

	@Override
	protected void validateErrorResponse() {
		callAndContinueOnFailure(EnsureErrorFromAuthorizationEndpointResponse.class, ConditionResult.FAILURE, "OID4VP-1FINAL-5");
		callAndContinueOnFailure(EnsureNoVpTokenInAuthorizationEndpointResponse.class, ConditionResult.FAILURE, "OID4VP-1FINAL-5");
		// OID4VP does not explicitly name an error code for a missing nonce, so only warn if the
		// wallet chose a code other than RFC6749's invalid_request
		callAndContinueOnFailure(EnsureInvalidRequestError.class, ConditionResult.WARNING, "OID4VP-1FINAL-5");
	}
}
