package net.openid.conformance.fapiciba;

import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-ciba-id1-client-test",
	displayName = "FAPI-CIBA-ID1: client test",
	summary = "Tests a 'happy path' flow; the client should perform OpenID discovery from the displayed discoveryUrl," +
		" call the backchannel endpoint, and then either Poll or get Pinged, after which the client will retrieve the" +
		" authentication result from the token endpoint.",
	profile = "FAPI-CIBA-ID1",
	configurationFields = {
		// TODO: Clean this up according to what we need for CIBA
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
