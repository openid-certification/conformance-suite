package net.openid.conformance.openid.client;

import net.openid.conformance.condition.as.OIDCCGenerateServerConfigurationIdTokenSigningAlgHS256Only;
import net.openid.conformance.condition.as.OIDCCGenerateServerConfigurationIdTokenSigningAlgRS256Only;
import net.openid.conformance.condition.as.SetServerSigningAlgToHS256;
import net.openid.conformance.condition.as.SetServerSigningAlgToRS256;
import net.openid.conformance.condition.as.SignIdTokenInvalid;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.as.OIDCCRegisterClientWithIdTokenSignedResponseAlgHS256;
import net.openid.conformance.sequence.as.OIDCCRegisterClientWithIdTokenSignedResponseAlgRS256;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "oidcc-client-test-invalid-sig-hs256",
	displayName = "OIDCC: Relying party test. Invalid id_token signature using HS256.",
	summary = "The client must identify the invalid signature and reject the ID Token after doing ID Token validation." +
		" Corresponds to rp-id_token-bad-sig-hs256 test in the old test suite",
	profile = "OIDCC",
	configurationFields = {
		"waitTimeoutSeconds"
	}
)
@VariantNotApplicable(parameter = ClientAuthType.class, values = {
	"none", "private_key_jwt"
})
public class OIDCCClientTestInvalidIdTokenSignatureWithHS256 extends AbstractOIDCCClientTestExpectingNothingInvalidIdToken {

	@Override
	public void customizeIdTokenSignature() {
		callAndStopOnFailure(SignIdTokenInvalid.class, "OIDCC-3.1.3.7", "OIDCC-3.2.2.11");
	}

	@Override
	protected void configureServerConfiguration() {
		callAndStopOnFailure(OIDCCGenerateServerConfigurationIdTokenSigningAlgHS256Only.class);
	}

	@Override
	protected void setServerSigningAlgorithm() {
		callAndStopOnFailure(SetServerSigningAlgToHS256.class);
	}

	@Override
	protected Class<? extends ConditionSequence> getAdditionalClientRegistrationSteps() {
		return OIDCCRegisterClientWithIdTokenSignedResponseAlgHS256.class;
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
