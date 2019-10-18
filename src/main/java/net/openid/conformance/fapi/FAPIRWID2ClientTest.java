package net.openid.conformance.fapi;

import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-rw-id2-client-test",
	displayName = "FAPI-RW-ID2: client test",
	summary = "Successful test case scenario where response_type used is code id_token combined with MTLS encryption",
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
