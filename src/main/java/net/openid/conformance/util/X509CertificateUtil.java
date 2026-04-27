package net.openid.conformance.util;

import com.nimbusds.jose.util.X509CertUtils;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Utility class for X.509 certificate operations.
 */
public class X509CertificateUtil {

	/**
	 * Checks if the given certificate is self-signed.
	 *
	 * A certificate is considered self-signed if it can be verified using its own public key.
	 *
	 * @param certificate the X.509 certificate to check
	 * @return true if the certificate is self-signed, false otherwise
	 */
	public static boolean isSelfSigned(X509Certificate certificate) {
		try {
			certificate.verify(certificate.getPublicKey());
			return true;
		} catch (Exception e) {
			// Verification failed - certificate is NOT self-signed
			return false;
		}
	}

	/**
	 * Parse x5c certificates from a list of Base64-encoded DER strings
	 * (as found in JSON-sourced x5c arrays).
	 *
	 * @throws X5cCertificateChainException if any certificate cannot be parsed
	 */
	public static List<X509Certificate> parseX5cCertificatesFromStrings(
		List<String> base64DerCertificates) throws X5cCertificateChainException {
		List<X509Certificate> certs = new ArrayList<>();
		for (int i = 0; i < base64DerCertificates.size(); i++) {
			byte[] der = java.util.Base64.getDecoder().decode(base64DerCertificates.get(i));
			X509Certificate cert = X509CertUtils.parse(der);
			if (cert == null) {
				throw new X5cCertificateChainException(
					"Failed to parse certificate at index " + i + " in x5c chain");
			}
			certs.add(cert);
		}
		return certs;
	}

	/**
	 * Parse x5c certificates from a list of Nimbus Base64 objects
	 * (as returned by JWSHeader.getX509CertChain()).
	 *
	 * @throws X5cCertificateChainException if any certificate cannot be parsed
	 */
	public static List<X509Certificate> parseX5cCertificatesFromNimbusBase64(
		List<com.nimbusds.jose.util.Base64> base64Certs) throws X5cCertificateChainException {
		List<X509Certificate> certs = new ArrayList<>();
		for (int i = 0; i < base64Certs.size(); i++) {
			X509Certificate cert = X509CertUtils.parse(base64Certs.get(i).decode());
			if (cert == null) {
				throw new X5cCertificateChainException(
					"Failed to parse certificate at index " + i + " in x5c chain");
			}
			certs.add(cert);
		}
		return certs;
	}

	/**
	 * Validate an x5c certificate chain (legacy / non-strict mode).
	 *
	 * Equivalent to {@link #validateX5cCertificateChain(List, X509Certificate, boolean)} with
	 * {@code strictPkix=false}.
	 *
	 * @param certs the parsed certificate chain, leaf first
	 * @param trustAnchor optional trust anchor certificate; null if not available
	 * @throws X5cCertificateChainException with a descriptive message if validation fails
	 */
	public static void validateX5cCertificateChain(List<X509Certificate> certs,
		X509Certificate trustAnchor) throws X5cCertificateChainException {
		validateX5cCertificateChain(certs, trustAnchor, false);
	}

	/**
	 * Validate an x5c certificate chain.
	 *
	 * In non-strict mode this performs leaf validity, leaf-not-self-signed, simple parent-signature
	 * walking, trust anchor exclusion, and (if a trust anchor is supplied) signature verification
	 * against the trust anchor.
	 *
	 * In strict mode this additionally performs full RFC 5280 PKIX path validation via
	 * {@link CertPathValidator}, which checks intermediate certificate validity windows,
	 * BasicConstraints CA:true on intermediates, KeyUsage keyCertSign on intermediates,
	 * name chaining, critical extensions, and other RFC 5280 requirements. A trust anchor is
	 * mandatory in strict mode. CRL/OCSP revocation checking is disabled (out of scope for the
	 * conformance suite).
	 *
	 * @param certs the parsed certificate chain, leaf first
	 * @param trustAnchor trust anchor certificate; mandatory in strict mode, optional otherwise
	 * @param strictPkix if true, perform RFC 5280 PKIX path validation
	 * @throws X5cCertificateChainException with a descriptive message if validation fails
	 */
	public static void validateX5cCertificateChain(List<X509Certificate> certs,
		X509Certificate trustAnchor, boolean strictPkix) throws X5cCertificateChainException {
		if (certs.isEmpty()) {
			throw new X5cCertificateChainException("x5c certificate chain is empty");
		}

		X509Certificate leafCert = certs.get(0);

		try {
			leafCert.checkValidity();
		} catch (CertificateExpiredException e) {
			throw new X5cCertificateChainException("Leaf certificate in x5c chain has expired");
		} catch (CertificateNotYetValidException e) {
			throw new X5cCertificateChainException("Leaf certificate in x5c chain is not yet valid");
		}

		if (isSelfSigned(leafCert)) {
			throw new X5cCertificateChainException("Leaf certificate in x5c chain must not be self-signed");
		}

		// Trust anchor exclusion check applies in both modes — HAIP / RFC 7515 forbid it.
		if (trustAnchor != null) {
			for (X509Certificate cert : certs) {
				if (cert.equals(trustAnchor)) {
					throw new X5cCertificateChainException(
						"Trust anchor certificate must not be included in x5c chain");
				}
			}
		}

		if (strictPkix) {
			if (trustAnchor == null) {
				throw new X5cCertificateChainException(
					"Strict PKIX validation requires a configured trust anchor");
			}
			validatePkixPath(certs, trustAnchor);
			return;
		}

		// Legacy non-strict path
		for (int i = 0; i < certs.size() - 1; i++) {
			try {
				certs.get(i).verify(certs.get(i + 1).getPublicKey());
			} catch (Exception e) {
				throw new X5cCertificateChainException(
					"Certificate chain verification failed: certificate at index " + i +
						" is not signed by certificate at index " + (i + 1) +
						": " + e.getMessage());
			}
		}

		if (trustAnchor != null) {
			X509Certificate lastCert = certs.get(certs.size() - 1);
			try {
				lastCert.verify(trustAnchor.getPublicKey());
			} catch (Exception e) {
				throw new X5cCertificateChainException(
					"Last certificate in x5c chain is not signed by the trust anchor: " + e.getMessage());
			}
		} else if (certs.size() > 1) {
			X509Certificate lastCert = certs.get(certs.size() - 1);
			if (isSelfSigned(lastCert)) {
				throw new X5cCertificateChainException(
					"Trust anchor (self-signed root CA) must not be included in x5c chain");
			}
		}
	}

	private static void validatePkixPath(List<X509Certificate> certs, X509Certificate trustAnchor)
		throws X5cCertificateChainException {
		try {
			CertPath certPath = CertificateFactory.getInstance("X.509").generateCertPath(certs);
			PKIXParameters params = new PKIXParameters(Set.of(new TrustAnchor(trustAnchor, null)));
			// Conformance suite tests cannot reach CRL/OCSP endpoints; revocation checking is out of scope.
			params.setRevocationEnabled(false);
			CertPathValidator.getInstance("PKIX").validate(certPath, params);
		} catch (CertPathValidatorException e) {
			String detail = e.getReason() != null ? e.getReason().toString() : e.getMessage();
			int idx = e.getIndex();
			String where = idx >= 0 ? " at chain index " + idx : "";
			throw new X5cCertificateChainException(
				"PKIX path validation failed" + where + ": " + detail);
		} catch (CertificateException | InvalidAlgorithmParameterException | NoSuchAlgorithmException e) {
			throw new X5cCertificateChainException(
				"PKIX path validation could not run: " + e.getMessage());
		}
	}

	/**
	 * Exception thrown when x5c certificate chain validation fails.
	 */
	@SuppressWarnings("serial")
	public static class X5cCertificateChainException extends Exception {
		public X5cCertificateChainException(String message) {
			super(message);
		}
	}
}
