package net.openid.conformance.condition.client;

import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.TestKeysAndCerts;

import java.util.Map;

/**
 * The OpenID4VCI issuer tests: without a 'Key Attestation JWKS' the suite signs the key
 * attestations an issuer asks for with its built-in wallet provider certificate.
 */
public class WarnIfBuiltInKeyAttestationSignerIsEphemeral extends AbstractWarnIfBuiltInCertificateAuthorityIsEphemeral {

	@Override
	protected Map<TestKeysAndCerts.SuiteCa, String> builtInSignersInUse(Environment env) {
		return isConfigured(env, "client_attestation.key_attestation_jwks", "vci.key_attestation_jwks")
			? Map.of()
			: Map.of(TestKeysAndCerts.SuiteCa.WALLET_PROVIDER, "the certificate signing the key attestations");
	}
}
