package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.as.ForceIdTokenToBeSignedWithAltRS256;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi2-security-profile-final-client-test-invalid-alternate-alg",
	displayName = "FAPI2-Security-Profile-Final: client test - the id_token is signed with RS256, which the client should not accept as it is expecting PS256, ES256, or EdDSA",
	summary = "This test should end with the client displaying an error message that the algorithm used to sign the id_token does not match the required algorithm",
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

public class FAPI2SPFinalClientTestInvalidAlternateAlg extends AbstractFAPI2SPFinalClientExpectNothingAfterIdTokenIssued {

	@Override
	protected void addCustomValuesToIdToken() {
		//Do Nothing
	}

	@Override
	protected void addCustomSignatureOfIdToken(){
		callAndStopOnFailure(ForceIdTokenToBeSignedWithAltRS256.class,"OIDCC-3.1.3.7-8");
	}

	@Override
	protected String getIdTokenFaultErrorMessage() {
		return "signed using alt RS256 alg.";
	}
}
