package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.as.AddAudValueAsArrayToIdToken;
import net.openid.conformance.condition.as.SignIdTokenBypassingNimbusChecks;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-ciba-id1-client-valid-aud-as-array-test",
	displayName = "FAPI-CIBA-ID1: Client test - valid aud in id_token as data type array",
	summary = "This test should be successful. The value of aud within the id_token will be represented as array with one value",
	profile = "FAPI-CIBA-ID1",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.scope",
		"client.backchannel_client_notification_endpoint",
		"client.certificate",
		"client.jwks"
	}
)
public class FAPICIBAClientValidAudAsArrayTest extends AbstractFAPICIBAClientTest {

	@Override
	protected void addCustomValuesToIdToken() {
		callAndStopOnFailure(AddAudValueAsArrayToIdToken.class,"OIDCC-3.1.3.7-3");
	}

	@Override
	protected void addCustomSignatureOfIdToken() {
		callAndStopOnFailure(SignIdTokenBypassingNimbusChecks.class);
	}

}
