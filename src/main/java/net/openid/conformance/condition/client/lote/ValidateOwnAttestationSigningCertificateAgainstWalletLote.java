package net.openid.conformance.condition.client.lote;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.TestKeysAndCerts;

import java.security.cert.X509Certificate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pre-flight check for the OpenID4VCI issuer tests with client attestation: the certificate the
 * suite signs its wallet attestations with (the configured 'Client Attester Keys JWKS' or the
 * built-in wallet provider certificate) must chain to a wallet provider in the configured wallet
 * providers list. Expected to be called as a WARNING.
 */
public class ValidateOwnAttestationSigningCertificateAgainstWalletLote extends AbstractValidateOwnCertificatesAgainstLote {

	@Override
	@PreEnvironment(required = { "config", LoteSlot.WALLET_ENV_KEY })
	public Environment evaluate(Environment env) {
		JsonElement configured = env.getElementFromObject("config", "client_attestation.attester_jwks");
		if (configured == null) {
			configured = env.getElementFromObject("config", "vci.client_attester_keys_jwks");
		}
		List<X509Certificate> chain = chainOfConfiguredJwks(configured, "'Client Attester Keys JWKS' field in the 'Client Attestation' section");
		Map<String, List<X509Certificate>> chains = new LinkedHashMap<>();
		if (chain == null) {
			chains.put("built-in wallet attestation signer", List.of(TestKeysAndCerts.getWalletAttestationSigner().getJwk().getParsedX509CertChain().get(0)));
		} else {
			chains.put("'Client Attester Keys JWKS' field in the 'Client Attestation' section", chain);
		}
		return check(env, chains, "wallet attestation signing", LoteSlot.WALLET);
	}
}
