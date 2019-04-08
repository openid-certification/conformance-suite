package io.fintechlabs.testframework.fapi;

import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.condition.as.SignIdTokenWithNullAlgorithm;
import io.fintechlabs.testframework.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-rw-id2-client-test-with-mtls-holder-of-key-invalid-null-alg",
	displayName = "FAPI-RW-ID2: client test - null algorithm used for serialization of id_token from authorization_endpoint, should be rejected (with MTLS)",
	summary = "This test should end with the client displaying an error message that the id_token was signed with alg: none",
	profile = "FAPI-RW-ID2",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.scope",
		"client.redirect_uri",
		"client.certificate",
		"client.jwks",
	}
)

public class FAPIRWClientTestCodeIdTokenWithMATLSInvalidNullAlg extends AbstractFAPIRWClientExpectNothingAfterAuthorisationEndpoint {

	@Override
	protected void addCustomValuesToIdToken() {

		//Do Nothing
	}

	@Override
	protected void addCustomSignatureOfIdToken(){

		callAndStopOnFailure(SignIdTokenWithNullAlgorithm.class,"OIDCC-3.1.3.7-7");
	}

	@Override
	protected Object authorizationCodeGrantType(String requestId) {

		throw new ConditionError(getId(), "Client has incorrectly called token_endpoint after receiving an id_token with a null alg value from the authorization_endpoint.");
	}

}
