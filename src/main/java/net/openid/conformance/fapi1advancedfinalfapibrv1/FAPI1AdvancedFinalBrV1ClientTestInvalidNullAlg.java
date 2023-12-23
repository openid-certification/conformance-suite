package net.openid.conformance.fapi1advancedfinalfapibrv1;

import net.openid.conformance.condition.as.SignIdTokenWithNullAlgorithm;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi1-advanced-final-br-v1-client-test-invalid-null-alg",
	displayName = "FAPI1-Advanced-Final-Br-v1: client test - null algorithm used for serialization of id_token from authorization_endpoint, should be rejected",
	summary = "This test should end with the client displaying an error message that the id_token was signed with alg: none",
	profile = "FAPI1-Advanced-Final-Br-v1",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.scope",
		"client.redirect_uri",
		"client.certificate",
		"client.jwks"
	}
)

public class FAPI1AdvancedFinalBrV1ClientTestInvalidNullAlg extends AbstractFAPI1AdvancedFinalBrV1ClientExpectNothingAfterIdTokenIssued {

	@Override
	protected void addCustomValuesToIdToken() {

		//Do Nothing
	}

	@Override
	protected void addCustomSignatureOfIdToken(){

		callAndStopOnFailure(SignIdTokenWithNullAlgorithm.class,"OIDCC-3.1.3.7-7");
	}

	@Override
	protected String getIdTokenFaultErrorMessage() {
		return "null alg value";
	}
}
