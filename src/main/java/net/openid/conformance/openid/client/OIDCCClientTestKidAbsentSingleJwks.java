package net.openid.conformance.openid.client;

import net.openid.conformance.condition.as.OIDCCGenerateServerJWKsSingleSigningKeyWithNoKeyId;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "oidcc-client-kid-absent-single-jwks",
	displayName = "OIDCC: Relying party test. Request an ID token and verify its signature using a single matching key provided by the Issuer.",
	summary = "Use the single matching key out of the Issuer's published set to verify the ID Tokens signature and accept the ID Token after doing ID Token validation." +
		" Corresponds to rp-id_token-kid-absent-single-jwks test in the old test suite",
	profile = "OIDCC",
	configurationFields = {
		"waitTimeoutSeconds"
	}
)
public class OIDCCClientTestKidAbsentSingleJwks extends AbstractOIDCCClientTestExpectingNothingInvalidIdToken {

	@Override
	protected void configureServerJWKS() {
		callAndStopOnFailure(OIDCCGenerateServerJWKsSingleSigningKeyWithNoKeyId.class);
	}

}
