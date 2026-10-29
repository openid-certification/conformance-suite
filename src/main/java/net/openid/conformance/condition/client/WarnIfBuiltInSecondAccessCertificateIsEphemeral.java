package net.openid.conformance.condition.client;

import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.TestKeysAndCerts;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The OpenID4VP wallet tests with multi-signed requests and the x509_hash client identifier prefix:
 * without a 'jwks' in the 'Second client' section the suite makes the second signature with its second
 * built-in access certificate ({@link AddBuiltInSecondAccessCertificateToClient2Configuration}). Only
 * reports that signer when the first one is configured; otherwise
 * {@link WarnIfBuiltInAccessCertificateIsEphemeral} already reports the access CA. Called before the
 * configuration is filled in.
 */
public class WarnIfBuiltInSecondAccessCertificateIsEphemeral extends AbstractWarnIfBuiltInCertificateAuthorityIsEphemeral {

	@Override
	protected Map<TestKeysAndCerts.SuiteCa, String> builtInSignersInUse(Environment env) {
		Map<TestKeysAndCerts.SuiteCa, String> inUse = new LinkedHashMap<>();
		if (isConfigured(env, "client.jwks") && !isConfigured(env, "client2.jwks")) {
			inUse.put(TestKeysAndCerts.SuiteCa.ACCESS, "the second access certificate making the second signature of the requests");
		}
		return inUse;
	}
}
