package net.openid.conformance.condition.client.lote;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.TestKeysAndCerts;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;

/**
 * Pre-flight check for the OpenID4VCI issuer tests: the certificate the suite signs the key
 * attestations an issuer asks for with (the configured 'Key Attestation JWKS' or the built-in
 * wallet provider certificate) must chain to a wallet provider in the configured wallet providers
 * list. Key attestations are issued under every client authentication method, so unlike
 * {@link ValidateOwnAttestationSigningCertificateAgainstWalletLote} this applies to every issuer
 * test. Expected to be called as a WARNING.
 */
public class ValidateOwnKeyAttestationSigningCertificateAgainstWalletLote extends AbstractValidateOwnCertificatesAgainstLote {

	private static final String FIELD = "'Key Attestation JWKS' field in the 'Client Attestation' section";

	@Override
	@PreEnvironment(required = "config")
	public Environment evaluate(Environment env) {
		if (noListConfigured(env, "key attestation signing", LoteSlot.WALLET)) {
			return env;
		}
		JsonElement configured = env.getElementFromObject("config", "client_attestation.key_attestation_jwks");
		if (configured == null) {
			configured = env.getElementFromObject("config", "vci.key_attestation_jwks");
		}
		List<X509Certificate> chain = chainOfConfiguredJwks(configured, FIELD);
		Map<String, List<X509Certificate>> chains = chain == null
			? Map.of("built-in key attestation signer", List.of(TestKeysAndCerts.getWalletAttestationSigner().getJwk().getParsedX509CertChain().get(0)))
			: Map.of(FIELD, chain);
		return check(env, chains, "key attestation signing", LoteSlot.WALLET);
	}
}
