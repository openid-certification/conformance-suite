package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.as.AddInvalidExpiredExpValueToIdToken;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-ciba-id1-client-invalid-expired-exp-test",
	displayName = "FAPI-CIBA-ID1: Client test - expired exp value in id_token; should be rejected",
	summary = "This test should end with the client displaying an error message that the exp value in the id_token from the token endpoint has expired more than 5 minutes in the past",
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
public class FAPICIBAClientInvalidExpiredExpTest extends AbstractFAPI1CIBAClientExpectNothingAfterIdTokenIssued {

	@Override
	protected void addCustomValuesToIdToken() {
		callAndStopOnFailure(AddInvalidExpiredExpValueToIdToken.class, "OIDCC-3.1.3.7-9");
	}

	@Override
	protected String getIdTokenFaultErrorMessage() {
		return "expired exp value";
	}

}
