package net.openid.conformance.condition.client;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jose.proc.JWSVerifierFactory;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jose.util.X509CertUtils;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.X509CertificateUtil;

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * HAIP-specific validation for Status List Tokens.
 *
 * Per HAIP section 6.1, the key used to validate the Status List Token MUST be
 * included in the x5c JOSE header. The signing certificate MUST NOT be self-signed
 * and the trust anchor MUST NOT be included in the x5c chain.
 */
public class ValidateCredentialValidityByStatusListIfPresentForHaip extends ValidateCredentialValidityByStatusListIfPresent {

	@Override
	protected void verifyStatusListTokenSignature(String statusListTokenJwtString, SignedJWT statusListTokenJwt, Environment env) {
		List<Base64> x5cChain = statusListTokenJwt.getHeader().getX509CertChain();
		if (x5cChain == null || x5cChain.isEmpty()) {
			throw error("Status List Token MUST contain an x5c JOSE header in HAIP",
				args("header", statusListTokenJwt.getHeader().toJSONObject()));
		}

		String trustAnchorPem = env.getString("status_list_trust_anchor_pem");
		X509Certificate trustAnchor = trustAnchorPem != null ? X509CertUtils.parse(trustAnchorPem) : null;

		List<X509Certificate> certs;
		try {
			certs = X509CertificateUtil.parseX5cCertificatesFromNimbusBase64(x5cChain);
			X509CertificateUtil.validateX5cCertificateChain(certs, trustAnchor);
		} catch (X509CertificateUtil.X5cCertificateChainException e) {
			throw error(e.getMessage(),
				args("header", statusListTokenJwt.getHeader().toJSONObject()));
		}

		X509Certificate leafCertificate = certs.get(0);
		try {
			PublicKey publicKey = leafCertificate.getPublicKey();
			JWSVerifierFactory factory = new DefaultJWSVerifierFactory();
			JWSVerifier verifier = factory.createJWSVerifier(statusListTokenJwt.getHeader(), publicKey);

			if (!statusListTokenJwt.verify(verifier)) {
				throw error("Failed to verify Status List Token signature using the leaf certificate from x5c",
					args("leaf_cert_subject", leafCertificate.getSubjectX500Principal().getName()));
			}
		} catch (JOSEException e) {
			throw error("Failed to verify Status List Token signature using x5c", e,
				args("leaf_cert_subject", leafCertificate.getSubjectX500Principal().getName()));
		}
	}
}
