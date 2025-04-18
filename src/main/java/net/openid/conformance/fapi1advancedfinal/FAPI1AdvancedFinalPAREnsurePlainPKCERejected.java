package net.openid.conformance.fapi1advancedfinal;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddCodeChallengeToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.CreatePlainCodeChallenge;
import net.openid.conformance.condition.client.EnsureInvalidRequestError;
import net.openid.conformance.condition.client.EnsurePARInvalidRequestError;
import net.openid.conformance.condition.client.ExpectPlainPkceErrorPage;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPIAuthRequestMethod;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi1-advanced-final-par-plain-pkce-rejected",
	displayName = "FAPI1-Advanced-Final: ensure plain pkce is rejected",
	summary = "This test makes a FAPI authorization request using plain PKCE (RFC7636), which must be rejected. FAPI1-Advanced-Final requires servers to reject PAR requests that do not use S256, clause 5.2.2-18. Depending on when the server chooses to verify the request, the refusal may be an error from the pushed authorization request endpoint, or an invalid_request error may be returned from the authorization endpoint, or an error may be shown to the user (a screenshot of which must be uploaded).",
	profile = "FAPI1-Advanced-Final",
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
@VariantNotApplicable(parameter = FAPIAuthRequestMethod.class, values = {
	"by_value" // PKCE is only required by FAPI1-Adv when using PAR
})
public class FAPI1AdvancedFinalPAREnsurePlainPKCERejected extends AbstractFAPI1AdvancedFinalPARExpectingAuthorizationEndpointPlaceholderOrCallback {

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
	protected void processParErrorResponse() {
		callAndContinueOnFailure(EnsurePARInvalidRequestError.class, Condition.ConditionResult.FAILURE, "RFC7636-4.4.1");
	}

	@Override
	protected void onAuthorizationCallbackResponse() {
		callAndContinueOnFailure(EnsureInvalidRequestError.class, Condition.ConditionResult.FAILURE, "RFC7636-4.4.1");
		fireTestFinished();
	}

}
