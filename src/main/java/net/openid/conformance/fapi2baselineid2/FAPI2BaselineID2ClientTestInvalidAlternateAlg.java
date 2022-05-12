package net.openid.conformance.fapi2baselineid2;

import net.openid.conformance.condition.as.ForceIdTokenToBeSignedWithRS256;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi2-baseline-id2-client-test-invalid-alternate-alg",
	displayName = "FAPI2-Baseline-ID2: client test - if the alg of id_token is PS256, then sign with RS256 in the authorization endpoint, should be rejected",
	summary = "This test should end with the client displaying an error message that the algorithm used to sign the id_token does not match the required algorithm",
	profile = "FAPI2-Baseline-ID2",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.scope",
		"client.redirect_uri",
		"client.certificate",
		"client.jwks"
	}
)

public class FAPI2BaselineID2ClientTestInvalidAlternateAlg extends AbstractFAPI2BaselineID2ClientExpectNothingAfterIdTokenIssued {

	@Override
	protected void addCustomValuesToIdToken() {

		//Do Nothing
	}

	@Override
	protected void addCustomSignatureOfIdToken(){

		callAndStopOnFailure(ForceIdTokenToBeSignedWithRS256.class,"OIDCC-3.1.3.7-8");
	}

	@Override
	protected String getIdTokenFaultErrorMessage() {
		return "signed using RS256 instead of PS256";
	}
}
