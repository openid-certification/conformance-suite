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

		X509Certificate leafCertificate = parseCertificate(x5cChain.get(0), "leaf certificate");
		if (X509CertificateUtil.isSelfSigned(leafCertificate)) {
			throw error("Status List Token signing certificate MUST NOT be self-signed in HAIP",
				args("leaf_cert_subject", leafCertificate.getSubjectX500Principal().getName(),
					"chain_length", x5cChain.size()));
		}

		if (x5cChain.size() > 1) {
			X509Certificate lastCertificate = parseCertificate(x5cChain.get(x5cChain.size() - 1), "last certificate");
			if (X509CertificateUtil.isSelfSigned(lastCertificate)) {
				throw error("Status List Token x5c chain MUST NOT include the trust anchor in HAIP",
					args("trust_anchor_subject", lastCertificate.getSubjectX500Principal().getName(),
						"chain_length", x5cChain.size()));
			}
		}

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

	private X509Certificate parseCertificate(Base64 encodedCertificate, String label) {
		X509Certificate certificate = X509CertUtils.parse(encodedCertificate.decode());
		if (certificate == null) {
			throw error("Failed to parse %s from Status List Token x5c header".formatted(label),
				args("x5c_certificate", encodedCertificate.toString()));
		}
		return certificate;
	}
}
