package net.openid.conformance.fapi1advancedfinal;

import net.openid.conformance.condition.as.ForceIdTokenToBeSignedWithRS256;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;

@PublishTestModule(
	testName = "fapi1-advanced-final-client-test-invalid-alternate-alg",
	displayName = "FAPI1-Advanced-Final: client test - if the alg of id_token is PS256, then sign with RS256 in the authorization endpoint, should be rejected",
	summary = "This test should end with the client displaying an error message that the algorithm used to sign the id_token does not match the required algorithm",
	profile = "FAPI1-Advanced-Final",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.scope",
		"client.redirect_uri",
		"client.certificate",
		"client.jwks",
	}
)

public class FAPI1AdvancedFinalClientTestInvalidAlternateAlg extends AbstractFAPI1AdvancedFinalClientExpectNothingAfterAuthorizationEndpoint {

	@Override
	protected void addCustomValuesToIdToken() {

		//Do Nothing
	}

	@Override
	protected void addCustomSignatureOfIdToken(){

		callAndStopOnFailure(ForceIdTokenToBeSignedWithRS256.class,"OIDCC-3.1.3.7-8");
	}

	@Override
	protected Object authorizationCodeGrantType(String requestId) {

		throw new TestFailureException(getId(), "Client has incorrectly called token_endpoint after receiving an id_token with an alg value of RS256 (it was originally PS256) from the authorization_endpoint.");
	}

}