package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.as.AddInvalidIssValueToIdToken;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-ciba-id1-client-invalid-iss-test",
	displayName = "FAPI-CIBA-ID1: Client test - invalid iss in id_token from the token endpoint; should be rejected",
	summary = "This test should end with the client displaying an error message that " +
		"the iss value in the id_token does not match the authorization server's issuer",
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
public class FAPICIBAClientInvalidIssTest extends AbstractFAPI1CIBAClientExpectNothingAfterIdTokenIssued {

	@Override
	protected void addCustomValuesToIdToken() {
		callAndStopOnFailure(AddInvalidIssValueToIdToken.class, "OIDCC-3.1.3.7-2");
	}

	@Override
	protected String getIdTokenFaultErrorMessage() {
		return "invalid iss value";
	}

}
