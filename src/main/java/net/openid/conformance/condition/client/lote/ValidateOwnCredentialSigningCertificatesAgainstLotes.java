package net.openid.conformance.condition.client.lote;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.TestKeysAndCerts;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pre-flight check for the modules in which the suite issues or presents credentials (the
 * OpenID4VCI wallet tests and the OpenID4VP verifier tests): the suite's mdoc document signer
 * certificate and its SD-JWT VC signing certificate (the configured 'Signing JWK' or the
 * built-in one) must chain to a PID or mDL provider in the configured lists. Runs when at least
 * one of the two lists is configured. Expected to be called as a WARNING.
 */
public class ValidateOwnCredentialSigningCertificatesAgainstLotes extends AbstractValidateOwnCertificatesAgainstLote {

	@Override
	@PreEnvironment(required = "config")
	public Environment evaluate(Environment env) {
		if (!env.containsObject(LoteSlot.PID.envKey) && !env.containsObject(LoteSlot.MDL.envKey)) {
			log("Neither a PID providers nor an mDL providers list of trusted entities is configured; nothing to check the suite's credential signing certificates against");
			return env;
		}
		Map<String, List<X509Certificate>> chains = new LinkedHashMap<>();
		chains.put("built-in mdoc document signer", List.of(javaCert(TestKeysAndCerts.getDocumentSignerCert())));
		List<X509Certificate> sdJwtSigner = chainOfConfiguredJwks(env.getElementFromObject("config", "credential.signing_jwk"),
			"'Signing JWK' field in the 'Credential Issuer' section");
		chains.put(sdJwtSigner == null ? "built-in SD-JWT VC signer" : "'Signing JWK' field in the 'Credential Issuer' section",
			sdJwtSigner == null ? List.of(javaCert(TestKeysAndCerts.getSdJwtPidSigner().getCert())) : sdJwtSigner);
		return check(env, chains, "credential signing", LoteSlot.PID, LoteSlot.MDL);
	}

	private X509Certificate javaCert(org.multipaz.crypto.X509Cert cert) {
		try {
			var encoded = cert.getEncoded();
			return (X509Certificate) CertificateFactory.getInstance("X.509")
				.generateCertificate(new ByteArrayInputStream(encoded.toByteArray(0, encoded.getSize())));
		} catch (Exception e) {
			throw error("Failed to convert the suite's own certificate", e);
		}
	}
}
