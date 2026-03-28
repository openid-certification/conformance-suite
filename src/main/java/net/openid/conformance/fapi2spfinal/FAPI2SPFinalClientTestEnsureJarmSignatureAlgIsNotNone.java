package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.as.SignJarmWithNullAlgorithm;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPIResponseMode;
import net.openid.conformance.variant.VariantNotApplicable;


@PublishTestModule(
	testName = "fapi2-security-profile-final-client-test-ensure-jarm-signature-is-not-none",
	displayName = "FAPI2-Security-Profile-Final: sends a JARM response with a signature signed with the none algorithm.",
	summary = "This test should end with the client displaying an error message that the JARM signature and/or algorithm is invalid",
	profile = "FAPI2-Security-Profile-Final",
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
public class FAPI2SPFinalClientTestEnsureJarmSignatureAlgIsNotNone extends AbstractFAPI2SPFinalClientExpectNothingAfterAuthorizationResponse {


	@Override
	protected void createJARMResponse() {
		generateJARMResponseClaims();
		callAndStopOnFailure(SignJarmWithNullAlgorithm.class);
		encryptJARMResponse();
	}


	@Override
	protected void addCustomValuesToIdToken(){
		//Do nothing
	}

	@Override
	protected String getAuthorizationResponseErrorMessage() {
		return "Signed JARM response with algorithm NONE";
	}
}
