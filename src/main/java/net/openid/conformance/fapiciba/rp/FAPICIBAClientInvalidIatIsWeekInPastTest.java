package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.as.AddIatValueIsWeekInPastToIdToken;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-ciba-id1-client-invalid-iat-is-week-in-past-test",
	displayName = "FAPI-CIBA-ID1: Client test - iat value which is a week in the past in id_token; should be rejected",
	summary = "This test should end with the client displaying an error message that the iat value in the id_token has expired",
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
public class FAPICIBAClientInvalidIatIsWeekInPastTest extends AbstractFAPI1CIBAClientExpectNothingAfterIdTokenIssued {

	@Override
	protected void addCustomValuesToIdToken() {
		callAndStopOnFailure(AddIatValueIsWeekInPastToIdToken.class, "OIDCC-3.1.3.7-10");
	}

	@Override
	protected String getIdTokenFaultErrorMessage() {
		return "iat value is a week in the past";
	}

}
