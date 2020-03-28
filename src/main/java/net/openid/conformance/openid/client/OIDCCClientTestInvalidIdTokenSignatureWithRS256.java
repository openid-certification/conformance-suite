package net.openid.conformance.openid.client;

import net.openid.conformance.condition.as.OIDCCGenerateServerConfigurationIdTokenSigningAlgRS256Only;
import net.openid.conformance.condition.as.SetServerSigningAlgToRS256;
import net.openid.conformance.condition.as.SignIdTokenInvalid;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.as.OIDCCRegisterClientWithIdTokenSignedResponseAlgRS256;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "oidcc-client-test-invalid-sig-rs256",
	displayName = "OIDCC: Relying party test. Invalid id_token signature using RS256.",
	summary = "The client must identify the invalid signature and reject the ID Token after doing ID Token validation." +
		" Corresponds to rp-id_token-bad-sig-rs256 test in the old test suite",
	profile = "OIDCC",
	configurationFields = {
		"waitTimeoutSeconds"
	}
)
public class OIDCCClientTestInvalidIdTokenSignatureWithRS256 extends AbstractOIDCCClientTestExpectingNothingInvalidIdToken {

	@Override
	public void customizeIdTokenSignature() {
		callAndStopOnFailure(SignIdTokenInvalid.class, "OIDCC-3.1.3.7", "OIDCC-3.2.2.11");
	}

	@Override
	protected void configureServerConfiguration() {
		callAndStopOnFailure(OIDCCGenerateServerConfigurationIdTokenSigningAlgRS256Only.class);
	}

	@Override
	protected void setServerSigningAlgorithm() {
		callAndStopOnFailure(SetServerSigningAlgToRS256.class);
	}

	@Override
	protected Class<? extends ConditionSequence> getAdditionalClientRegistrationSteps() {
		return OIDCCRegisterClientWithIdTokenSignedResponseAlgRS256.class;
	}


	@Override
	protected String getAuthorizationCodeGrantTypeErrorMessage() {
		return "Client has incorrectly called token_endpoint after receiving an id_token with an invalid signature from the authorization_endpoint.";
	}

	@Override
	protected String getHandleUserinfoEndpointRequestErrorMessage() {
		return "Client has incorrectly called userinfo_endpoint after receiving an id_token with an invalid signature.";
	}

}
