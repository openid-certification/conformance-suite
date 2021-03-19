package net.openid.conformance.fapirwid2;

import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-rw-id2-client-test",
	displayName = "FAPI-RW-ID2: client test",
	summary = "Tests a 'happy path' flow; the client should perform OpenID discovery from the displayed discoveryUrl, call the authorization endpoint (which will immediately redirect back), exchange the authorization code for an access token at the token endpoint and make a GET request to the accounts endpoint displayed.",
	profile = "FAPI-RW-ID2",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.scope",
		"client.redirect_uri",
		"client.certificate",
		"client.jwks",
	}
)

public class FAPIRWID2ClientTest extends AbstractFAPIRWID2ClientTest {

	@Override
	protected void addCustomValuesToIdToken(){
		//Do nothing
	}
}
