package net.openid.conformance.condition;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jose.proc.JWSVerifierFactory;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.util.X509CertificateUtil;

import java.security.PublicKey;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
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
		List<X509Certificate> certs = new ArrayList<>();
		for (int i = 0; i < base64Certs.size(); i++) {
			X509Certificate cert = X509CertUtils.parse(base64Certs.get(i).decode());
			if (cert == null) {
				throw error("Failed to parse certificate at index " + i + " in x5c chain",
					args("index", i, "encoded_cert", base64Certs.get(i).toString()));
			}
			certs.add(cert);
		}
		return certs;
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
		if (certs.isEmpty()) {
			throw error("x5c certificate chain is empty");
		}

		X509Certificate leafCert = certs.get(0);

		// Check leaf certificate validity dates
		try {
			leafCert.checkValidity();
		} catch (CertificateExpiredException e) {
			throw error("Leaf certificate in x5c chain has expired",
				args("leaf_cert_subject", leafCert.getSubjectX500Principal().getName(),
					"not_after", leafCert.getNotAfter()));
		} catch (CertificateNotYetValidException e) {
			throw error("Leaf certificate in x5c chain is not yet valid",
				args("leaf_cert_subject", leafCert.getSubjectX500Principal().getName(),
					"not_before", leafCert.getNotBefore()));
		}

		// Leaf must not be self-signed
		if (X509CertificateUtil.isSelfSigned(leafCert)) {
			throw error("Leaf certificate in x5c chain must not be self-signed",
				args("leaf_cert_subject", leafCert.getSubjectX500Principal().getName()));
		}

		// Walk the chain: each cert must be signed by the next
		for (int i = 0; i < certs.size() - 1; i++) {
			try {
				certs.get(i).verify(certs.get(i + 1).getPublicKey());
			} catch (Exception e) {
				throw error("Certificate chain verification failed: certificate at index " + i +
						" is not signed by certificate at index " + (i + 1),
					args("cert_subject", certs.get(i).getSubjectX500Principal().getName(),
						"expected_issuer_subject", certs.get(i + 1).getSubjectX500Principal().getName()));
			}
		}

		if (trustAnchor != null) {
			// Trust anchor must not appear in the chain
			for (int i = 0; i < certs.size(); i++) {
				if (certs.get(i).equals(trustAnchor)) {
					throw error("Trust anchor certificate must not be included in x5c chain",
						args("trust_anchor_subject", trustAnchor.getSubjectX500Principal().getName(),
							"found_at_index", i));
				}
			}

			// Last cert in chain must be signed by the trust anchor
			X509Certificate lastCert = certs.get(certs.size() - 1);
			try {
				lastCert.verify(trustAnchor.getPublicKey());
			} catch (Exception e) {
				throw error("Last certificate in x5c chain is not signed by the trust anchor",
					args("last_cert_subject", lastCert.getSubjectX500Principal().getName(),
						"trust_anchor_subject", trustAnchor.getSubjectX500Principal().getName()));
			}
		} else if (certs.size() > 1) {
			// Without a trust anchor, check that the last cert is not self-signed
			// (self-signed last cert indicates the trust anchor was included)
			X509Certificate lastCert = certs.get(certs.size() - 1);
			if (X509CertificateUtil.isSelfSigned(lastCert)) {
				throw error("Trust anchor (self-signed root CA) must not be included in x5c chain",
					args("trust_anchor_subject", lastCert.getSubjectX500Principal().getName(),
						"chain_length", certs.size()));
			}
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
