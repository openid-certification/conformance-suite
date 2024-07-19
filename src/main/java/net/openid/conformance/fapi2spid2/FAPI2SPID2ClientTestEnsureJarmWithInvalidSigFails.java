package net.openid.conformance.fapi2spid2;

import net.openid.conformance.condition.as.InvalidateJarmSignature;
import net.openid.conformance.condition.as.jarm.SignJARMResponse;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPIResponseMode;
import net.openid.conformance.variant.VariantNotApplicable;


@PublishTestModule(
	testName = "fapi2-security-profile-id2-client-test-ensure-jarm-with-invalid-sig-fails",
	displayName = "FAPI2-Security-Profile-ID2: sends a JARM response with an invalid signature.",
	summary = "This test should end with the client displaying an error message that the JARM response has an invalid signature",
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

@VariantNotApplicable(parameter = FAPIResponseMode.class, values = { "plain_response" })
public class FAPI2SPID2ClientTestEnsureJarmWithInvalidSigFails extends AbstractFAPI2SPID2ClientExpectNothingAfterAuthorizationResponse {


	@Override
	protected void createJARMResponse() {
		generateJARMResponseClaims();
		callAndStopOnFailure(SignJARMResponse.class,"JARM-2.2");
		callAndStopOnFailure(InvalidateJarmSignature.class);
		encryptJARMResponse();
	}


	@Override
	protected void addCustomValuesToIdToken(){
		//Do nothing
	}

	@Override
	protected String getAuthorizationResponseErrorMessage() {
		return "Signed JARM response with an invalid signature";
	}
}
