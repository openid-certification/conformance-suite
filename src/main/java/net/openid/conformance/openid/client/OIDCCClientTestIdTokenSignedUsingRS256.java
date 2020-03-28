package net.openid.conformance.openid.client;

import net.openid.conformance.condition.as.OIDCCGenerateServerConfigurationIdTokenSigningAlgRS256Only;
import net.openid.conformance.condition.as.SetServerSigningAlgToRS256;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.as.OIDCCRegisterClientWithIdTokenSignedResponseAlgRS256;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "oidcc-client-test-idtoken-sig-rs256",
	displayName = "OIDCC: Relying party test. Id token signed using RS256",
	summary = "The client is expected to accept an id_token signed using RS256." +
		" Corresponds to rp-id_token-sig-rs256 test in the old test suite.",
	profile = "OIDCC",
	configurationFields = {
	}
)
public class OIDCCClientTestIdTokenSignedUsingRS256 extends AbstractOIDCCClientTest {

	@Override
	protected void setServerSigningAlgorithm() {
		callAndStopOnFailure(SetServerSigningAlgToRS256.class, "OIDCR-2");
	}

	@Override
	protected void configureServerConfiguration() {
		callAndStopOnFailure(OIDCCGenerateServerConfigurationIdTokenSigningAlgRS256Only.class);
	}

	@Override
	protected Class<? extends ConditionSequence> getAdditionalClientRegistrationSteps() {
		return OIDCCRegisterClientWithIdTokenSignedResponseAlgRS256.class;
	}
}
