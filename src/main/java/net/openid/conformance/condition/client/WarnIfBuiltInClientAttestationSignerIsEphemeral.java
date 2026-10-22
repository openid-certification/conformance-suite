package net.openid.conformance.condition.client;

import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.TestKeysAndCerts;

import java.util.Map;

/**
 * The OpenID4VCI issuer tests with client attestation: without a 'Client Attester Keys JWKS' the
 * suite signs its client attestations with its built-in wallet provider certificate.
 */
public class WarnIfBuiltInClientAttestationSignerIsEphemeral extends AbstractWarnIfBuiltInCertificateAuthorityIsEphemeral {

	@Override
	protected Map<TestKeysAndCerts.SuiteCa, String> builtInSignersInUse(Environment env) {
		return isConfigured(env, "client_attestation.attester_jwks", "vci.client_attester_keys_jwks")
			? Map.of()
			: Map.of(TestKeysAndCerts.SuiteCa.WALLET_PROVIDER, "the certificate signing the client attestations");
	}
}
