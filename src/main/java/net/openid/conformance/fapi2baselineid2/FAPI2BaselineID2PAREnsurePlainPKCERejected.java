package net.openid.conformance.fapi2baselineid2;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddCodeChallengeToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.CreatePlainCodeChallenge;
import net.openid.conformance.condition.client.EnsureInvalidRequestError;
import net.openid.conformance.condition.client.EnsurePARInvalidRequestError;
import net.openid.conformance.condition.client.ExpectPlainPkceErrorPage;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi2-baseline-id2-par-plain-pkce-rejected",
	displayName = "FAPI2-Baseline-ID2: ensure plain pkce is rejected",
	summary = "This test makes a FAPI authorization request using plain PKCE (RFC7636), which must be rejected. FAPI2-Baseline-ID2 requires servers to reject PAR requests that do not use S256, clause 5.2.2-18. Depending on when the server chooses to verify the request, the refusal may be an error from the pushed authorization request endpoint, or an invalid_request error may be returned from the authorization endpoint, or an error may be shown to the user (a screenshot of which must be uploaded).",
	profile = "FAPI2-Baseline-ID2",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"client2.client_id",
		"client2.scope",
		"client2.jwks",
		"mtls2.key",
		"mtls2.cert",
		"mtls2.ca",
		"resource.resourceUrl"
	}
)
public class FAPI2BaselineID2PAREnsurePlainPKCERejected extends AbstractFAPI2BaselineID2ExpectingAuthorizationEndpointPlaceholderOrCallback {

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectPlainPkceErrorPage.class, "FAPI1-ADV-5.2.2-18");

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
	protected void processParResponse() {
		// the server could reject this at the par endpoint, or at the authorization endpoint
		String key = "pushed_authorization_endpoint_response_http_status";
		Integer http_status = env.getInteger(key);
		if (http_status >= 200 && http_status < 300) {
			super.processParResponse();
			return;
		}

		callAndContinueOnFailure(EnsurePARInvalidRequestError.class, Condition.ConditionResult.FAILURE, "RFC7636-4.4.1");

		fireTestFinished();
	}

	@Override
	protected void onAuthorizationCallbackResponse() {
		callAndContinueOnFailure(EnsureInvalidRequestError.class, Condition.ConditionResult.FAILURE, "RFC7636-4.4.1");
		fireTestFinished();
	}

}
