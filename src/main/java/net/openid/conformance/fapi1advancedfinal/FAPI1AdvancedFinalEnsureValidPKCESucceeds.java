package net.openid.conformance.fapi1advancedfinal;

import net.openid.conformance.condition.client.AddCodeVerifierToTokenEndpointRequest;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.client.SetupPkceAndAddToAuthorizationRequest;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPIAuthRequestMethod;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi1-advanced-final-ensure-valid-pkce-succeeds",
	displayName = "FAPI1-Advanced-Final: ensure valid pkce succeeds",
	summary = "This test makes a FAPI authorization request using valid PKCE (RFC7636), which must succeed. FAPI1-Advanced-Final does not require servers to support PKCE, but as per https://tools.ietf.org/html/rfc6749#section-3.1 'The authorization server MUST ignore unrecognized request parameters' - i.e. whether the server supports PKCE or not, a valid PKCE request must succeed. The reason for this test is that many OpenID Connect clients speculatively use PKCE, and the OAuth2 standard requires that requests from such clients must not fail.",
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
	"pushed" // All PAR requests must use PKCE, so we don't need a separate test valid PKCE for PAR
})
public class FAPI1AdvancedFinalEnsureValidPKCESucceeds extends AbstractFAPI1AdvancedFinalServerTestModule {

	@Override
	protected ConditionSequence makeCreateAuthorizationRequestSteps() {
		return super.makeCreateAuthorizationRequestSteps()
			.then(new SetupPkceAndAddToAuthorizationRequest());
	}

	@Override
	protected void createAuthorizationCodeRequest() {
		super.createAuthorizationCodeRequest();

		callAndStopOnFailure(AddCodeVerifierToTokenEndpointRequest.class, "RFC7636-4.5");
	}
}
