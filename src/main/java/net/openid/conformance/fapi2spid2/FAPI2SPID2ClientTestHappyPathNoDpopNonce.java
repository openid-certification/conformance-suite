package net.openid.conformance.fapi2spid2;

import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI2SenderConstrainMethod;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi2-security-profile-id2-client-test-happy-path-no-dpop-nonce",
	displayName = "FAPI2-Security-Profile-ID2: client test for DPOP happy path that does not use DPOP nonce",
	summary = "Tests a 'happy path' flow; the client should perform OpenID discovery from the displayed discoveryUrl, call the authorization endpoint (which will immediately redirect back), exchange the authorization code for an access token at the token endpoint and make a GET request to the resource endpoint displayed (usually the 'accounts' or 'userinfo' endpoint).",
	profile = "FAPI2-Security-Profile-ID2",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.scope",
		"client.redirect_uri",
		"client.certificate",
		"client.jwks",
		"waitTimeoutSeconds"
	}
)

@VariantNotApplicable(parameter = FAPI2SenderConstrainMethod.class, values = {
	"mtls"
})
public class FAPI2SPID2ClientTestHappyPathNoDpopNonce extends FAPI2SPID2ClientTestHappyPath {
	@Override
	protected boolean requireAuthorizationServerEndpointDpopNonce() {
		return false;
	}

	@Override
	protected boolean requireResourceServerEndpointDpopNonce() {
		return false;
	}
}
