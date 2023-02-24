package net.openid.conformance.fapirwid2;

import net.openid.conformance.condition.as.RemoveAtHashFromIdToken;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-rw-id2-client-test-missing-athash",
	displayName = "FAPI-RW-ID2: client test - a happy flow test where the returned id_token will not have an at_hash value",
	summary = "The returned id_token will not have an at_hash value. at_hash is optional as an access token is not being returned from the authorization endpoint, so the flow should succeed even though at_hash is absent.",
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
public class FAPIRWID2ClientTestNoAtHash extends AbstractFAPIRWID2ClientTest {

	@Override
	protected void addCustomValuesToIdToken() {
		callAndStopOnFailure(RemoveAtHashFromIdToken.class, "OIDCC-3.3.2.9");
	}

}
