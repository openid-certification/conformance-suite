package net.openid.conformance.openid;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddNonceToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.CheckErrorFromAuthorizationEndpointErrorInvalidRequest;
import net.openid.conformance.condition.client.ExpectRequestMissingNonceErrorPage;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ResponseType;
import net.openid.conformance.variant.VariantNotApplicable;

// Equivalent of https://www.heenan.me.uk/~joseph/oidcc_test_desc-phase1.html#OP_nonce_NoReq_noncode
@PublishTestModule(
	testName = "oidcc-ensure-request-without-nonce-fails",
	displayName = "OIDCC: ensure request without nonce fails",
	summary = "This test sends a request without a nonce included, and should end with the authorization server showing an error message that the request is invalid (a screenshot of which should be uploaded) or with the user being redirected back to the conformance suite with an invalid_request error (in both cases due to the missing nonce). nonce is required for all flows that return an id_token from the authorization endpoint, see https://openid.net/specs/openid-connect-core-1_0.html#ImplicitAuthRequest or https://openid.net/specs/openid-connect-core-1_0.html#HybridIDToken and https://bitbucket.org/openid/connect/issues/972/nonce-requirement-in-hybrid-auth-request. This is clear in the latest OpenID Connect errata draft, https://openid.net/specs/openid-connect-core-1_0-27.html#NonceNotes",
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
	protected ConditionSequence createAuthorizationRequestSequence() {
		return super.createAuthorizationRequestSequence()
			.skip(AddNonceToAuthorizationEndpointRequest.class,
				"NOT adding nonce to request object");
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
