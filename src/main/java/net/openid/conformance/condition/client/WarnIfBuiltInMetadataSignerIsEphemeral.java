package net.openid.conformance.condition.client;

import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.TestKeysAndCerts;

import java.util.Map;

/**
 * The emulated credential issuer's signed metadata: without a 'Signing JWK' in the 'Credential
 * Issuer' section the suite signs it with its built-in wallet-relying-party access certificate.
 */
public class WarnIfBuiltInMetadataSignerIsEphemeral extends AbstractWarnIfBuiltInCertificateAuthorityIsEphemeral {

	@Override
	protected Map<TestKeysAndCerts.SuiteCa, String> builtInSignersInUse(Environment env) {
		return isConfigured(env, "credential.signing_jwk")
			? Map.of()
			: Map.of(TestKeysAndCerts.SuiteCa.ACCESS, "the access certificate signing the credential issuer metadata, replaced by 'Signing JWK' in the 'Credential Issuer' section");
	}
}
