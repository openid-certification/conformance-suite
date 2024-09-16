package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.as.AddUntrustedSecondAudValueToIdToken;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-ciba-id1-client-invalid-secondary-aud-test",
	displayName = "FAPI-CIBA-ID1: Client test - untrusted aud value in id_token from the token endpoint; must be rejected",
	summary = """
		This test issues an id_token where the 'aud' is an array which contains both the correct client_id \
		and the id for a non-existent client. This test should end with the client displaying an error message that \
		there is an untrusted aud value in the id_token from the authorization_endpoint, or that the 'azp' claim is missing.

		As per OpenID Connect section 3.1.3.7 clause 3:

		'The ID Token MUST be rejected if the ID Token does not list the Client as a valid audience, \
		or if it contains additional audiences not trusted by the Client.'

		 and clause 4:

		'If the ID Token contains multiple audiences, the Client SHOULD verify that an azp Claim is present.'\
		""",
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
public class FAPICIBAClientInvalidSecondaryAudTest extends AbstractFAPI1CIBAClientExpectNothingAfterIdTokenIssued {

	@Override
	protected void addCustomValuesToIdToken() {
		callAndStopOnFailure(AddUntrustedSecondAudValueToIdToken.class, "OIDCC-3.1.3.7-3");
	}

	@Override
	protected String getIdTokenFaultErrorMessage() {
		return "aud is an array that contains an untrusted value";
	}

}
