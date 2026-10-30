package net.openid.conformance.condition.client;

import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.TestKeysAndCerts;

import java.util.Map;

/**
 * The modules that issue or present SD-JWT VCs: without a 'Signing JWK' in the 'Credential Issuer'
 * section the suite signs the credentials and their status list with its built-in PID provider
 * certificate. Not for mdoc credentials: the IACA root's key is built in, so it is never ephemeral.
 */
public class WarnIfBuiltInCredentialSignerIsEphemeral extends AbstractWarnIfBuiltInCertificateAuthorityIsEphemeral {

	@Override
	protected Map<TestKeysAndCerts.SuiteCa, String> builtInSignersInUse(Environment env) {
		return isConfigured(env, "credential.signing_jwk")
			? Map.of()
			: Map.of(TestKeysAndCerts.SuiteCa.PID_PROVIDER, "the certificate signing the SD-JWT VC credentials and their status list, replaced by 'Signing JWK' in the 'Credential Issuer' section");
	}
}
