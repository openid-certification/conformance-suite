package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.as.RemoveAudFromIdToken;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-ciba-id1-client-invalid-missing-aud-test",
	displayName = "FAPI-CIBA-ID1: Client test - missing aud value in id_token in id_token; should be rejected",
	summary = "This test should end with the client displaying an error message that the aud value in the id_token is missing",
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
public class FAPICIBAClientInvalidMissingAudTest extends AbstractFAPI1CIBAClientExpectNothingAfterIdTokenIssued {

	@Override
	protected void addCustomValuesToIdToken() {
		callAndStopOnFailure(RemoveAudFromIdToken.class, "OIDCC-3.1.3.7-3");
	}

	@Override
	protected String getIdTokenFaultErrorMessage() {
		return "missing aud value";
	}

}
