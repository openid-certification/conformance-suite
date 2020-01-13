package net.openid.conformance.openid;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddNonceToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.CheckErrorFromAuthorizationEndpointErrorInvalidRequest;
import net.openid.conformance.condition.client.ExpectRequestMissingNonceErrorPage;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ResponseType;
import net.openid.conformance.variant.VariantNotApplicable;

// Equivalent of https://www.heenan.me.uk/~joseph/oidcc_test_desc-phase1.html#OP_nonce_NoReq_noncode
@PublishTestModule(
	testName = "oidcc-ensure-request-without-nonce-fails",
	displayName = "OIDCC: ensure request without nonce fails",
	summary = "This test should end with the authorisation server showing an error message that the request is invalid (a screenshot of which should be uploaded) or with the user being redirected back to the conformance suite with a correct error response.",
	profile = "OIDCC",
	configurationFields = {
		"server.discoveryUrl"
	}
)
@VariantNotApplicable(parameter = ResponseType.class, values = {"code", "code token"})
public class OIDCCEnsureRequestWithoutNonceFails extends AbstractOIDCCServerTestExpectingAuthorizationEndpointPlaceholderOrCallback {

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectRequestMissingNonceErrorPage.class, "OIDCC-3.2.2.1");
		env.putString("error_callback_placeholder", env.getString("invalid_request_error"));
	}

	@Override
	protected void createAuthorizationRequest() {
		call(new CreateAuthorizationRequestSteps()
				.skip(AddNonceToAuthorizationEndpointRequest.class,
						"NOT adding nonce to request object"));
	}

	@Override
	protected void processCallback() {
		eventLog.startBlock(currentClientString() + "Verify authorization endpoint response");

		env.mapKey("authorization_endpoint_response", "callback_params");

		performGenericAuthorizationEndpointErrorResponseValidation();
		callAndContinueOnFailure(CheckErrorFromAuthorizationEndpointErrorInvalidRequest.class,
				Condition.ConditionResult.FAILURE,
				"OIDCC-3.2.2.1");

		eventLog.endBlock();
		fireTestFinished();
	}
}
