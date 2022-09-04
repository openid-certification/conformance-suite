package net.openid.conformance.openid;

import net.openid.conformance.condition.client.AddCodeVerifierToTokenEndpointRequest;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.client.SetupPkceAndAddToAuthorizationRequest;
import net.openid.conformance.testmodule.PublishTestModule;

// New test not present in python suite
@PublishTestModule(
	testName = "oidcc-ensure-request-with-valid-pkce-succeeds",
	displayName = "OIDCC: ensure request with valid PKCE succeeds",
	summary = "This test makes an authorization request using valid PKCE (RFC7636), which must succeed. OpenID Connect does not require servers to support PKCE (although it is recommended by the OAuth2 security BCP), but as per https://tools.ietf.org/html/rfc6749#section-3.1 'The authorization server MUST ignore unrecognized request parameters' - i.e. whether the server supports PKCE or not, a valid PKCE request must succeed. The reason for this test is that many OpenID Connect clients speculatively use PKCE, and the OAuth2 standard requires that requests from such clients must not fail.",
	profile = "OIDCC"
)
public class OIDCCEnsureRequestWithValidPkceSucceeds extends AbstractOIDCCServerTest {

	@Override
	protected ConditionSequence createAuthorizationRequestSequence() {
		return super.createAuthorizationRequestSequence()
			.then(new SetupPkceAndAddToAuthorizationRequest());
	}

	@Override
	protected void createAuthorizationCodeRequest() {
		super.createAuthorizationCodeRequest();

		callAndStopOnFailure(AddCodeVerifierToTokenEndpointRequest.class, "RFC7636-4.5");
	}

}
