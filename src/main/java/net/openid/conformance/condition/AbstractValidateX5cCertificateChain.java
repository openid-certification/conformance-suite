package net.openid.conformance.condition;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jose.proc.JWSVerifierFactory;
import com.nimbusds.jose.util.X509CertUtils;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.util.X509CertificateUtil;

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.List;

/**
 * Abstract base condition for validating x5c certificate chains.
 *
 * Provides shared helper methods for:
 * - Parsing x5c certificate lists from Base64 DER strings or Nimbus Base64 objects
 * - Validating the chain: validity dates, self-signed checks, chain signature walking,
 *   trust anchor exclusion
 * - Verifying a JWT signature using the leaf certificate from the chain
 *
 * Per RFC 7515 section 4.1.6, the leaf certificate (containing the key used to sign)
 * MUST be the first certificate. Subsequent certificates each certify the previous one.
 * Per HAIP, the trust anchor MUST NOT be included in the chain and the leaf MUST NOT
 * be self-signed.
 */
public abstract class AbstractValidateX5cCertificateChain extends AbstractCondition {

	/**
	 * Parse x5c certificates from a list of Base64-encoded DER strings
	 * (as found in JSON-sourced x5c arrays).
	 */
	protected List<X509Certificate> parseX5cCertificatesFromStrings(List<String> base64DerCerts) {
		try {
			return X509CertificateUtil.parseX5cCertificatesFromStrings(base64DerCerts);
		} catch (X509CertificateUtil.X5cCertificateChainException e) {
			throw error(e.getMessage());
		}
	}

	/**
	 * Parse x5c certificates from a list of Nimbus Base64 objects
	 * (as returned by JWSHeader.getX509CertChain()).
	 */
	protected List<X509Certificate> parseX5cCertificatesFromNimbusBase64(List<com.nimbusds.jose.util.Base64> base64Certs) {
		try {
			return X509CertificateUtil.parseX5cCertificatesFromNimbusBase64(base64Certs);
		} catch (X509CertificateUtil.X5cCertificateChainException e) {
			throw error(e.getMessage());
		}
	}

	/**
	 * Parse a PEM-encoded trust anchor certificate. Returns {@code null} when the input
	 * is {@code null}; throws if the input is non-null but not a parseable X.509 certificate
	 * (so a misconfigured trust anchor surfaces as a test failure rather than silently
	 * downgrading to legacy chain validation).
	 */
	protected X509Certificate parseTrustAnchorPem(String trustAnchorPem) {
		if (trustAnchorPem == null) {
			return null;
		}
		X509Certificate cert = X509CertUtils.parse(trustAnchorPem);
		if (cert == null) {
			throw error("Configured trust anchor PEM could not be parsed as an X.509 certificate");
		}
		return cert;
	}

	/**
	 * Validate an x5c certificate chain.
	 *
	 * When a trust anchor is supplied, performs full RFC 5280 PKIX path validation
	 * (intermediate validity, BasicConstraints CA:true on intermediates, KeyUsage keyCertSign
	 * on intermediates, name chaining, critical extensions; CRL/OCSP revocation checking is
	 * disabled). When no trust anchor is supplied, performs only the legacy chain walk
	 * (parent-signature walk + trust-anchor-exclusion check on the last cert).
	 *
	 * In both modes: chain must be non-empty, leaf must be within validity dates and not
	 * self-signed, and the trust anchor (when supplied) must not appear in the chain.
	 *
	 * The fail-fast "configure a trust anchor" UX for HAIP plans lives in the
	 * {@code Ensure*TrustAnchorConfigured} preconditions wired into the relevant test-module
	 * HAIP branch at setup time, not in this helper.
	 *
	 * @param certs the parsed certificate chain, leaf first
	 * @param trustAnchor trust anchor certificate; non-null triggers strict PKIX validation
	 */
	protected void validateX5cCertificateChain(List<X509Certificate> certs, X509Certificate trustAnchor) {
		try {
			X509CertificateUtil.validateX5cCertificateChain(certs, trustAnchor);
		} catch (X509CertificateUtil.X5cCertificateChainException e) {
			throw error(e.getMessage());
		}
	}

	/**
	 * Verify a JWT signature using the leaf certificate's public key from the x5c chain.
	 *
	 * @param jwtString the raw JWT string
	 * @param certs the parsed x5c certificate chain (leaf first)
	 */
	protected void verifyJwtSignatureWithX5cLeafCert(String jwtString, List<X509Certificate> certs) {
		X509Certificate leafCert = certs.get(0);
		PublicKey publicKey = leafCert.getPublicKey();

		try {
			SignedJWT jwt = SignedJWT.parse(jwtString);
			JWSVerifierFactory factory = new DefaultJWSVerifierFactory();
			JWSVerifier verifier = factory.createJWSVerifier(jwt.getHeader(), publicKey);

			if (!jwt.verify(verifier)) {
				throw error("JWT signature verification failed using the leaf certificate from x5c",
					args("leaf_cert_subject", leafCert.getSubjectX500Principal().getName()));
			}
		} catch (ParseException e) {
			throw error("Failed to parse JWT for signature verification", e,
				args("jwt", jwtString));
		} catch (JOSEException e) {
			throw error("Error verifying JWT signature using x5c leaf certificate", e,
				args("leaf_cert_subject", leafCert.getSubjectX500Principal().getName()));
		}
	}
}
