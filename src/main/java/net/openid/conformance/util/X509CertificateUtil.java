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
import java.security.cert.CertificateParsingException;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Utility class for X.509 certificate operations.
 */
public class X509CertificateUtil {

	/**
	 * Encodes a certificate as PEM, e.g. for inclusion in a log entry's detail so the full
	 * certificate a check ran against can be inspected. Returns a description of the problem
	 * instead of throwing when the certificate cannot be re-encoded.
	 */
	public static String toPem(X509Certificate certificate) {
		try {
			return "-----BEGIN CERTIFICATE-----\n"
				+ java.util.Base64.getMimeEncoder(64, "\n".getBytes(java.nio.charset.StandardCharsets.US_ASCII))
					.encodeToString(certificate.getEncoded())
				+ "\n-----END CERTIFICATE-----";
		} catch (CertificateException e) {
			return "certificate could not be encoded: " + e.getMessage();
		}
	}

	// KeyUsage bit positions, in the order X509Certificate.getKeyUsage() returns them
	private static final String[] KEY_USAGE_NAMES = {
		"digitalSignature", "nonRepudiation", "keyEncipherment", "dataEncipherment",
		"keyAgreement", "keyCertSign", "cRLSign", "encipherOnly", "decipherOnly"
	};

	/**
	 * Decodes a certificate into a human readable multi-line description (subject, issuer,
	 * serial, validity, key, key identifiers, key usage, extensions), for inclusion in a log
	 * entry's detail next to the PEM so the certificate a check ran against can be read without
	 * feeding the PEM to another tool. Best effort: a part that cannot be decoded is reported
	 * in place rather than failing the description.
	 */
	public static String describe(X509Certificate cert) {
		StringBuilder sb = new StringBuilder();
		sb.append("subject: ").append(cert.getSubjectX500Principal().getName()).append('\n');
		sb.append("issuer: ").append(cert.getIssuerX500Principal().getName()).append('\n');
		sb.append("serial (hex): ").append(cert.getSerialNumber().toString(16)).append('\n');
		sb.append("version: v").append(cert.getVersion()).append('\n');
		sb.append("notBefore: ").append(cert.getNotBefore().toInstant()).append('\n');
		sb.append("notAfter: ").append(cert.getNotAfter().toInstant()).append('\n');
		sb.append("signature algorithm: ").append(cert.getSigAlgName()).append('\n');
		sb.append("public key algorithm: ").append(cert.getPublicKey().getAlgorithm()).append('\n');
		sb.append("subjectKeyIdentifier: ").append(keyIdentifier(cert, "2.5.29.14", false)).append('\n');
		sb.append("authorityKeyIdentifier: ").append(keyIdentifier(cert, "2.5.29.35", true)).append('\n');
		boolean[] keyUsage = cert.getKeyUsage();
		if (keyUsage == null) {
			sb.append("keyUsage: <absent>\n");
		} else {
			List<String> usages = new ArrayList<>();
			for (int i = 0; i < keyUsage.length && i < KEY_USAGE_NAMES.length; i++) {
				if (keyUsage[i]) {
					usages.add(KEY_USAGE_NAMES[i]);
				}
			}
			sb.append("keyUsage").append(isCritical(cert, "2.5.29.15") ? " (critical)" : "")
				.append(": ").append(String.join(", ", usages)).append('\n');
		}
		try {
			List<String> eku = cert.getExtendedKeyUsage();
			sb.append("extendedKeyUsage").append(isCritical(cert, "2.5.29.37") ? " (critical)" : "")
				.append(": ").append(eku == null ? "<absent>" : String.join(", ", eku)).append('\n');
		} catch (CertificateException e) {
			sb.append("extendedKeyUsage: <could not be decoded: ").append(e.getMessage()).append(">\n");
		}
		int bc = cert.getBasicConstraints();
		sb.append("basicConstraints").append(isCritical(cert, "2.5.29.19") ? " (critical)" : "")
			.append(": ").append(!hasExtension(cert, "2.5.29.19") ? "<absent>"
				: bc == -1 ? "CA:FALSE" : "CA:TRUE" + (bc == Integer.MAX_VALUE ? "" : ", pathlen:" + bc))
			.append('\n');
		Collection<List<?>> ian;
		try {
			ian = cert.getIssuerAlternativeNames();
			if (ian != null) {
				sb.append("issuerAlternativeName: ").append(ian).append('\n');
			}
		} catch (CertificateParsingException e) {
			sb.append("issuerAlternativeName: <could not be decoded: ").append(e.getMessage()).append(">\n");
		}
		byte[] crlDp = cert.getExtensionValue("2.5.29.31");
		sb.append("cRLDistributionPoints: ").append(crlDp == null ? "<absent>" : "present").append('\n');
		sb.append("critical extension OIDs: ").append(oids(cert.getCriticalExtensionOIDs())).append('\n');
		sb.append("non-critical extension OIDs: ").append(oids(cert.getNonCriticalExtensionOIDs()));
		return sb.toString();
	}

	private static boolean hasExtension(X509Certificate cert, String oid) {
		return cert.getExtensionValue(oid) != null;
	}

	private static boolean isCritical(X509Certificate cert, String oid) {
		Set<String> critical = cert.getCriticalExtensionOIDs();
		return critical != null && critical.contains(oid);
	}

	private static String oids(Set<String> oids) {
		return oids == null || oids.isEmpty() ? "<none>" : String.join(", ", new java.util.TreeSet<>(oids));
	}

	private static String keyIdentifier(X509Certificate cert, String oid, boolean authority) {
		byte[] value = cert.getExtensionValue(oid);
		if (value == null) {
			return "<absent>";
		}
		try {
			org.bouncycastle.asn1.ASN1Primitive parsed =
				org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils.parseExtensionValue(value);
			byte[] keyId = authority
				? org.bouncycastle.asn1.x509.AuthorityKeyIdentifier.getInstance(parsed).getKeyIdentifierOctets()
				: org.bouncycastle.asn1.x509.SubjectKeyIdentifier.getInstance(parsed).getKeyIdentifier();
			if (keyId == null) {
				return "<present, but no keyIdentifier field>";
			}
			StringBuilder hex = new StringBuilder(keyId.length * 2);
			for (byte b : keyId) {
				hex.append(String.format("%02x", b));
			}
			return hex.toString();
		} catch (Exception e) {
			return "<could not be decoded: " + e.getMessage() + ">";
		}
	}

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
	 * Validate an x5c certificate chain.
	 *
	 * Always performs leaf validity, leaf-not-self-signed, and trust-anchor-exclusion checks.
	 *
	 * When a trust anchor is supplied, performs full RFC 5280 PKIX path validation via
	 * {@link CertPathValidator}: intermediate certificate validity windows, BasicConstraints
	 * CA:true on intermediates, KeyUsage keyCertSign on intermediates, name chaining, critical
	 * extensions. Callers wanting strict PKIX must therefore configure a trust anchor; the
	 * conditions framework surfaces that requirement via {@code Ensure*TrustAnchorConfigured}
	 * preconditions wired into the relevant test-module HAIP branch. CRL/OCSP revocation
	 * checking is disabled (out of scope for the conformance suite).
	 *
	 * When no trust anchor is supplied, performs the legacy walk only (parent-signature walk
	 * up the chain plus a "trust anchor MUST NOT be in the chain" self-signed-last-cert check).
	 *
	 * @param certs the parsed certificate chain, leaf first
	 * @param trustAnchor trust anchor certificate; non-null triggers strict PKIX validation
	 * @throws X5cCertificateChainException with a descriptive message if validation fails
	 */
	public static void validateX5cCertificateChain(List<X509Certificate> certs,
		X509Certificate trustAnchor) throws X5cCertificateChainException {
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

		if (trustAnchor != null) {
			for (X509Certificate cert : certs) {
				if (cert.equals(trustAnchor)) {
					throw new X5cCertificateChainException(
						"Trust anchor certificate must not be included in x5c chain");
				}
			}
			validatePkixPath(certs, trustAnchor);
			return;
		}

		// No trust anchor: legacy walk only.
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

		if (certs.size() > 1) {
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
