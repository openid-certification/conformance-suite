package net.openid.conformance.fapi2spid2;

import net.openid.conformance.condition.as.SignIdTokenWithNullAlgorithm;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi2-securityprofile-id2-client-test-invalid-null-alg",
	displayName = "FAPI2-SecurityProfile-ID2: client test - null algorithm used for serialization of id_token from token_endpoint, should be rejected",
	summary = "This test should end with the client displaying an error message that the id_token was signed with alg: none",
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

public class FAPI2BaselineID2ClientTestInvalidNullAlg extends AbstractFAPI2BaselineID2ClientExpectNothingAfterIdTokenIssued {

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
