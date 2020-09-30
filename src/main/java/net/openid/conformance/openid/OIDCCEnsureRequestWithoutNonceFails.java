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
	summary = "This test sends a request without a nonce included, and should end with the authorization server showing an error message that the request is invalid (a screenshot of which should be uploaded) or with the user being redirected back to the conformance suite with a correct error response. nonce is required for all flows that return an id_token from the authorization endpoint.",
	profile = "OIDCC"
)
@VariantNotApplicable(parameter = ResponseType.class, values = {"code", "code token"})
public class OIDCCEnsureRequestWithoutNonceFails extends AbstractOIDCCServerTestExpectingAuthorizationEndpointPlaceholderOrCallback {

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectRequestMissingNonceErrorPage.class, "OIDCC-3.2.2.1", "OIDCC-3.3.2.11");
		env.putString("error_callback_placeholder", env.getString("invalid_request_error"));
	}

	@Override
	protected void createAuthorizationRequest() {
		call(new CreateAuthorizationRequestSteps(formPost)
				.skip(AddNonceToAuthorizationEndpointRequest.class,
						"NOT adding nonce to request object"));
	}

	@Override
	protected void onAuthorizationCallbackResponse() {
		performGenericAuthorizationEndpointErrorResponseValidation();
		callAndContinueOnFailure(CheckErrorFromAuthorizationEndpointErrorInvalidRequest.class,
			Condition.ConditionResult.FAILURE,
			"OIDCC-3.2.2.1", "OIDCC-3.3.2.11");
		fireTestFinished();
	}

}
