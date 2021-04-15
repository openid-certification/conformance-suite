package net.openid.conformance.fapirwid2;

import net.openid.conformance.condition.as.AddAudValueAsArrayToIdToken;
import net.openid.conformance.condition.as.SignIdTokenBypassingNimbusChecks;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-rw-id2-client-test-valid-aud-as-array",
	displayName = "FAPI-RW-ID2: client test - valid aud in id_token as data type array",
	summary = "This test should be successful. The value of aud within the id_token will be represented as array with one value",
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

public class FAPIRWID2ClientTestValidAudAsArray extends AbstractFAPIRWID2ClientTest {

	@Override
	protected void addCustomValuesToIdToken(){

		callAndStopOnFailure(AddAudValueAsArrayToIdToken.class,"OIDCC-3.1.3.7-3");
	}

	@Override
	protected void addCustomSignatureOfIdToken(){

		callAndStopOnFailure(SignIdTokenBypassingNimbusChecks.class);
	}
}
