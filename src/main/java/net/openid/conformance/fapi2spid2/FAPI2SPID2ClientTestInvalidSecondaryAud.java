package net.openid.conformance.fapi2spid2;

import net.openid.conformance.condition.as.AddUntrustedSecondAudValueToIdToken;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi2-security-profile-id2-client-test-invalid-secondary-aud",
	displayName = "FAPI2-Security-Profile-ID2: client test - untrusted aud value in id_token from token_endpoint, must be rejected",
	summary = "This test issues an id_token where the 'aud' is an array which contains both the correct client_id and the id for a non-existent client. This test should end with the client displaying an error message that there is an untrusted aud value in the id_token from the token_endpoint, or that the 'azp' claim is missing.\n\nAs per OpenID Connect section 3.1.3.7 clause 3:\n\n'The ID Token MUST be rejected if the ID Token does not list the Client as a valid audience, or if it contains additional audiences not trusted by the Client.'\n\n and clause 4:\n\n'If the ID Token contains multiple audiences, the Client SHOULD verify that an azp Claim is present.'",
	profile = "FAPI2-Security-Profile-ID2",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.scope",
		"client.redirect_uri",
		"client.certificate",
		"client.jwks",
		"waitTimeoutSeconds"
	}
)

public class FAPI2SPID2ClientTestInvalidSecondaryAud extends AbstractFAPI2SPID2ClientExpectNothingAfterIdTokenIssued {

	@Override
	protected void addCustomValuesToIdToken() {

		callAndStopOnFailure(AddUntrustedSecondAudValueToIdToken.class, "OIDCC-3.1.3.7-3");
	}

	@Override
	protected String getIdTokenFaultErrorMessage() {
		return "aud is an array that contains an untrusted value";
	}
}
