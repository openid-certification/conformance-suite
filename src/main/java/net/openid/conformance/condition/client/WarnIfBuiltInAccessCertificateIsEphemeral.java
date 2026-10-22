package net.openid.conformance.condition.client;

import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.TestKeysAndCerts;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The OpenID4VP wallet tests with an x509 client identifier prefix: without a 'jwks' in the 'Client'
 * section the suite signs requests with its built-in wallet-relying-party access certificate
 * ({@link AddBuiltInAccessCertificateToClientConfiguration}) and, without a 'verifier_info', sends
 * its built-in registration certificate ({@link AddBuiltInRegistrationCertificateToClientConfiguration}).
 * Called before those two fill the configuration in.
 */
public class WarnIfBuiltInAccessCertificateIsEphemeral extends AbstractWarnIfBuiltInCertificateAuthorityIsEphemeral {

	@Override
	protected Map<TestKeysAndCerts.SuiteCa, String> builtInSignersInUse(Environment env) {
		Map<TestKeysAndCerts.SuiteCa, String> inUse = new LinkedHashMap<>();
		if (!isConfigured(env, "client.jwks")) {
			inUse.put(TestKeysAndCerts.SuiteCa.ACCESS, "the access certificate signing the requests");
			if (!isConfigured(env, "client.verifier_info")) {
				inUse.put(TestKeysAndCerts.SuiteCa.WRPRC_PROVIDER, "the registration certificate sent with the requests");
			}
		}
		return inUse;
	}
}
