package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddCodeChallengeToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.CreatePlainCodeChallenge;
import net.openid.conformance.condition.client.EnsureInvalidRequestError;
import net.openid.conformance.condition.client.EnsurePARInvalidRequestError;
import net.openid.conformance.condition.client.ExpectPlainPkceErrorPage;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi2-security-profile-final-par-plain-pkce-rejected",
	displayName = "FAPI2-Security-Profile-Final: ensure plain pkce is rejected",
	summary = "This test makes a FAPI authorization request using plain PKCE (RFC7636), which must be rejected. FAPI2-Security-Profile-Final requires servers to reject PAR requests that do not use S256, clause 5.2.2-18. Depending on when the server chooses to verify the request, the refusal may be an error from the pushed authorization request endpoint, or an invalid_request error may be returned from the authorization endpoint, or an error may be shown to the user (a screenshot of which must be uploaded).",
	profile = "FAPI2-Security-Profile-Final",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.jwks",
		"client2.client_id",
		"client2.scope",
		"client2.jwks",
		"resource.resourceUrl"
	}
)
public class FAPI2SPFinalPAREnsurePlainPKCERejected extends AbstractFAPI2SPFinalPARExpectingAuthorizationEndpointPlaceholderOrCallback {

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectPlainPkceErrorPage.class, "FAPI2-SP-FINAL-5.3.2.2-5");

		env.putString("error_callback_placeholder", env.getString("plain_pkce_error"));
	}

	@Override
	protected ConditionSequence makeCreateAuthorizationRequestSteps() {
		// ideally we'd replace the CreateS256CodeChallenge call instead, but
		// .replace() doesn't currently work for sequences-within-sequences
		return super.makeCreateAuthorizationRequestSteps()
			.then(condition(CreatePlainCodeChallenge.class))
			.then(condition(AddCodeChallengeToAuthorizationEndpointRequest.class));
	}

	@Override
	protected void processParErrorResponse() {
		callAndContinueOnFailure(EnsurePARInvalidRequestError.class, Condition.ConditionResult.FAILURE, "RFC7636-4.4.1");
	}

	@Override
	protected void onAuthorizationCallbackResponse() {
		callAndContinueOnFailure(EnsureInvalidRequestError.class, Condition.ConditionResult.FAILURE, "RFC7636-4.4.1");
		fireTestFinished();
	}

}
