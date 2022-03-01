package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.fapi1advancedfinal.AbstractFAPI1AdvancedFinalClientTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-ciba-id1-client-test",
	displayName = "FAPI-CIBA-ID1: client test",
	summary = "Tests a 'happy path' flow; the client should perform OpenID discovery from the displayed discoveryUrl, call the authorization endpoint (which will immediately redirect back), exchange the authorization code for an access token at the token endpoint and make a GET request to the accounts endpoint displayed.",
	profile = "FAPI-CIBA-ID1",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.scope",
		"client.redirect_uri",
		"client.certificate",
		"client.jwks",
		"directory.keystore"
	}
)

public class FAPICIBAID1ClientTest extends AbstractFAPICIBAID1ClientTest {

	@Override
	protected void addCustomValuesToIdToken(){
		//Do nothing
	}
}
