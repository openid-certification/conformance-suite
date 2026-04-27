package net.openid.conformance.condition;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jose.proc.JWSVerifierFactory;
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
	 * Validate an x5c certificate chain.
	 *
	 * Performs the following checks:
	 * <ol>
	 *   <li>Chain must not be empty</li>
	 *   <li>Leaf certificate validity dates (checkValidity())</li>
	 *   <li>Leaf certificate must NOT be self-signed</li>
	 *   <li>Chain signature walking: each cert[i] is verified by cert[i+1]'s public key</li>
	 *   <li>If trustAnchor is provided: trust anchor must not appear in the chain,
	 *       and the last cert must be signed by the trust anchor</li>
	 *   <li>If trustAnchor is null and chain has more than one cert: last cert must not
	 *       be self-signed (trust anchor exclusion)</li>
	 * </ol>
	 *
	 * @param certs the parsed certificate chain, leaf first
	 * @param trustAnchor optional trust anchor certificate; null if not available
	 */
	protected void validateX5cCertificateChain(List<X509Certificate> certs, X509Certificate trustAnchor) {
		validateX5cCertificateChain(certs, trustAnchor, false);
	}

	/**
	 * Validate an x5c certificate chain, optionally with strict RFC 5280 PKIX path validation.
	 *
	 * In strict mode the trust anchor is mandatory and full PKIX checks are performed
	 * (intermediate validity, BasicConstraints CA:true, KeyUsage keyCertSign, name chaining,
	 * critical extensions). CRL/OCSP revocation checking is disabled.
	 *
	 * @param certs the parsed certificate chain, leaf first
	 * @param trustAnchor trust anchor certificate; mandatory in strict mode, optional otherwise
	 * @param strictPkix if true, perform RFC 5280 PKIX path validation
	 */
	protected void validateX5cCertificateChain(List<X509Certificate> certs, X509Certificate trustAnchor, boolean strictPkix) {
		try {
			X509CertificateUtil.validateX5cCertificateChain(certs, trustAnchor, strictPkix);
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
