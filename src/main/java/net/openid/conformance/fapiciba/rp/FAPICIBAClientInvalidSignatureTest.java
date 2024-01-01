package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.as.InvalidateIdTokenSignature;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-ciba-id1-client-invalid-signature-test",
	displayName = "FAPI-CIBA-ID1: Client test - invalid signature in id_token from the token endpoint; should be rejected",
	summary = "This test should end with the client displaying an error message that the signature in the id_token is invalid",
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
public class FAPICIBAClientInvalidSignatureTest extends AbstractFAPI1CIBAClientExpectNothingAfterIdTokenIssued {

	@Override
	protected void addCustomSignatureOfIdToken(){
		callAndStopOnFailure(InvalidateIdTokenSignature.class, "OIDCC-3.1.3.7-6");

	}

	@Override
	protected String getIdTokenFaultErrorMessage() {
		return "invalid signature";
	}

}
