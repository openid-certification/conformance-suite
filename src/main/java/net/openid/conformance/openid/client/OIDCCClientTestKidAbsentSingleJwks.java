package net.openid.conformance.openid.client;

import net.openid.conformance.condition.as.OIDCCGenerateServerJWKsSingleSigningKeyWithNoKeyId;
import net.openid.conformance.condition.as.SetServerSigningAlgToRS256;
import net.openid.conformance.condition.client.ValidateServerJWKs;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "oidcc-client-test-kid-absent-single-jwks",
	displayName = "OIDCC: Relying party test. Request an ID token and verify its signature using a single matching RSA key provided by the Issuer." +
		" Use of RS256 algorithm is required for this test.",
	summary = "Use the single matching key out of the Issuer's published set to verify the ID Tokens signature and accept the ID Token after doing ID Token validation." +
		" Corresponds to rp-id_token-kid-absent-single-jwks test in the old test suite",
	profile = "OIDCC",
	configurationFields = {
		"waitTimeoutSeconds"
	}
)
public class OIDCCClientTestKidAbsentSingleJwks extends AbstractOIDCCClientTest {

	@Override
	protected void configureServerJWKS() {
		callAndStopOnFailure(OIDCCGenerateServerJWKsSingleSigningKeyWithNoKeyId.class, "OIDCC-10.1");
	}

	@Override
	protected void setServerSigningAlgorithm() {
		callAndStopOnFailure(SetServerSigningAlgToRS256.class);
	}

	@Override
	protected void validateConfiguredServerJWKS() {
		callAndStopOnFailure(ValidateServerJWKs.class, "RFC7517-1.1");
	}
}
