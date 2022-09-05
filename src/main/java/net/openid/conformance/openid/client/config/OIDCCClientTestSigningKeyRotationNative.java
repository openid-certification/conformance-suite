package net.openid.conformance.openid.client.config;

import net.openid.conformance.openid.client.AbstractOIDCCClientTest;
import net.openid.conformance.testmodule.PublishTestModule;


@PublishTestModule(
	testName = "oidcc-client-test-signing-key-rotation-just-before-signing",
	displayName = "OIDCC: Relying party signing key rotation test",
	summary = "The client is expected to request an ID token and verify its signature by" +
		" fetching keys from the jwks endpoint. Keys will be rotated right before signing the id_token" +
		" so the client needs to refetch the jwks to validate the signature." +
		"Corresponds to rp-key-rotation-op-sign-key-native test in the old test suite.",
	profile = "OIDCC",
	configurationFields = {
	}
)
//TODO needs a better class and test name. why the Python test was called "-native" is unclear
public class OIDCCClientTestSigningKeyRotationNative extends AbstractOIDCCClientTest {


	@Override
	protected void signIdToken() {
		//generate a new jwks with new kids
		super.configureServerJWKS();
		super.signIdToken();
	}

}
