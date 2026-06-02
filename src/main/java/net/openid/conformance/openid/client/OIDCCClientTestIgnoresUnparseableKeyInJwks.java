package net.openid.conformance.openid.client;

import net.openid.conformance.condition.as.AddUnparseableKeyToServerPublicJwks;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "oidcc-client-test-ignores-unparseable-key-in-jwks",
	displayName = "OIDCC: Relying party test. The Issuer's published JWKS contains an additional key the"
		+ " relying party cannot parse; verify the RP ignores it and verifies the ID Token using the"
		+ " usable key.",
	summary = "The Issuer publishes its real signing key alongside two unusable keys (each with a distinct"
		+ " kid): a post-quantum-shaped key (kty=AKP with a non-existent ML-DSA parameter set) and a key"
		+ " with a made-up key type. A conformant relying party must ignore the keys it cannot use and"
		+ " verify the ID Token signature using the real key, as per RFC 7517 section 5 (a recipient SHOULD"
		+ " ignore keys whose values are out of the supported ranges). This is the steady state during any"
		+ " algorithm transition - e.g. an Issuer adding post-quantum keys before all relying parties"
		+ " support them.",
	profile = "OIDCC",
	configurationFields = {
		"waitTimeoutSeconds"
	}
)
public class OIDCCClientTestIgnoresUnparseableKeyInJwks extends AbstractOIDCCClientTest {

	@Override
	protected void configureServerJWKS() {
		super.configureServerJWKS();
		callAndStopOnFailure(AddUnparseableKeyToServerPublicJwks.class, "RFC7517-5");
	}
}
