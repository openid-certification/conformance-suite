package net.openid.conformance.fapi2spid2;

import net.openid.conformance.condition.as.AddInvalidAudValueToJarm;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPIResponseMode;
import net.openid.conformance.variant.VariantNotApplicable;


@PublishTestModule(
	testName = "fapi2-securityprofile-id2-client-test-ensure-jarm-with-invalid-aud-fails",
	displayName = "FAPI2-SecurityProfile-ID2: sends a JARM response with an invalid aud claim.",
	summary = "This test should end with the client displaying an error message that the JARM response has an invalid aud claim",
	profile = "FAPI2-SecurityProfile-ID2",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.scope",
		"client.redirect_uri",
		"client.certificate",
		"client.jwks"
	}
)

@VariantNotApplicable(parameter = FAPIResponseMode.class, values = { "plain_response" })
public class FAPI2SPID2ClientTestEnsureJarmWithInvalidAudFails extends AbstractFAPI2SPID2ClientExpectNothingAfterAuthorizationResponse {


	@Override
	protected void addCustomValuesToJarmResponse() {
		callAndContinueOnFailure(AddInvalidAudValueToJarm.class, "JARM-4.1");
	}

	@Override
	protected void addCustomValuesToIdToken(){
		//Do nothing
	}

	@Override
	protected String getAuthorizationResponseErrorMessage() {
		return "Added invalid aud to JARM response";
	}
}
