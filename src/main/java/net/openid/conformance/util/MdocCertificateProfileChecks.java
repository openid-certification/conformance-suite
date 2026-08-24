package net.openid.conformance.util;

import org.bouncycastle.asn1.ASN1BitString;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1PrintableString;
import org.bouncycastle.asn1.ASN1UTF8String;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.asn1.x509.CRLDistPoint;
import org.bouncycastle.asn1.x509.DistributionPoint;
import org.bouncycastle.asn1.x509.DistributionPointName;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Shared certificate profile checks for the ISO/IEC 18013-5 Annex B certificate profiles
 * (Table B.1 IACA root, Table B.3 document signer). Each check appends human-readable
 * violation strings to the caller's list; the calling condition decides severity.
 */
public final class MdocCertificateProfileChecks {

	private MdocCertificateProfileChecks() {
		// utility class
	}

	public static final Set<String> ECDSA_SIGNATURE_ALGORITHM_OIDS = Set.of(
		"1.2.840.10045.4.3.2", // ecdsa-with-SHA256
		"1.2.840.10045.4.3.3", // ecdsa-with-SHA384
		"1.2.840.10045.4.3.4"  // ecdsa-with-SHA512
	);

	private static final String OID_EC_PUBLIC_KEY = "1.2.840.10045.2.1";
	private static final String OID_ED25519 = "1.3.101.112";
	private static final String OID_ED448 = "1.3.101.113";

	private static final Map<String, String> ALLOWED_EC_CURVES = Map.of(
		"1.2.840.10045.3.1.7", "P-256",
		"1.3.132.0.34", "P-384",
		"1.3.132.0.35", "P-521",
		"1.3.36.3.3.2.8.1.1.7", "brainpoolP256r1",
		"1.3.36.3.3.2.8.1.1.9", "brainpoolP320r1",
		"1.3.36.3.3.2.8.1.1.11", "brainpoolP384r1",
		"1.3.36.3.3.2.8.1.1.13", "brainpoolP512r1"
	);

	// ISO 18013-5 B.1.1: "The following extensions shall not be used"
	private static final Map<String, String> FORBIDDEN_EXTENSIONS = Map.of(
		"2.5.29.33", "PolicyMappings",
		"2.5.29.30", "NameConstraints",
		"2.5.29.36", "PolicyConstraints",
		"2.5.29.54", "InhibitAnyPolicy",
		"2.5.29.46", "FreshestCRL"
	);

	private static final int MAX_SERIAL_OCTETS = 20;

	public static X509CertificateHolder holderOf(X509Certificate cert) {
		try {
			return new X509CertificateHolder(cert.getEncoded());
		} catch (IOException | CertificateEncodingException e) {
			throw new IllegalArgumentException("Failed to re-encode certificate", e);
		}
	}

	public static void checkVersionAndSerial(X509Certificate cert, List<String> violations) {
		if (cert.getVersion() != 3) {
			violations.add("certificate version is " + cert.getVersion() + " but must be v3");
		}
		if (cert.getSerialNumber().signum() != 1) {
			violations.add("serial number must be a positive, non-zero integer");
		} else if (cert.getSerialNumber().toByteArray().length > MAX_SERIAL_OCTETS) {
			violations.add("serial number is longer than the maximum of 20 octets");
		}
	}

	public static void checkValidity(X509Certificate cert, int maxValidityDays, List<String> violations) {
		long validityMillis = cert.getNotAfter().getTime() - cert.getNotBefore().getTime();
		if (validityMillis > Duration.ofDays(maxValidityDays).toMillis()) {
			violations.add("validity period exceeds the maximum of " + maxValidityDays + " days after notBefore");
		}
		Date now = new Date();
		if (now.before(cert.getNotBefore())) {
			violations.add("certificate is not yet valid (notBefore is in the future)");
		}
		if (now.after(cert.getNotAfter())) {
			violations.add("certificate has expired");
		}
	}

	public static void checkSignatureAlgorithm(X509Certificate cert, List<String> violations) {
		if (!ECDSA_SIGNATURE_ALGORITHM_OIDS.contains(cert.getSigAlgOID())) {
			violations.add("signature algorithm " + cert.getSigAlgOID()
				+ " is not one of the permitted ECDSA-with-SHA256/SHA384/SHA512 algorithms");
		}
		// Table rows for 'Signature' (4.1.2.3): value shall match the OID in the signature algorithm
		X509CertificateHolder holder = holderOf(cert);
		ASN1ObjectIdentifier tbsAlgorithm =
			holder.toASN1Structure().getTBSCertificate().getSignature().getAlgorithm();
		ASN1ObjectIdentifier outerAlgorithm = holder.getSignatureAlgorithm().getAlgorithm();
		if (!tbsAlgorithm.equals(outerAlgorithm)) {
			violations.add("the signature field inside the TBSCertificate (" + tbsAlgorithm
				+ ") does not match the outer signature algorithm (" + outerAlgorithm + ")");
		}
	}

	/**
	 * Checks countryName (mandatory, upper case two-letter, PrintableString), commonName
	 * (mandatory) and that all subject DirectoryString attributes use PrintableString or
	 * UTF8String encoding.
	 */
	public static void checkSubjectAttributes(X509Certificate cert, List<String> violations) {
		X500Name subject = X500Name.getInstance(cert.getSubjectX500Principal().getEncoded());

		RDN[] countryRdns = subject.getRDNs(BCStyle.C);
		if (countryRdns.length == 0) {
			violations.add("subject does not contain a countryName attribute");
		} else {
			ASN1Encodable countryValue = countryRdns[0].getFirst().getValue();
			String country = IETFUtils.valueToString(countryValue);
			if (!country.matches("[A-Z]{2}")) {
				violations.add("subject countryName '" + country
					+ "' is not an upper case two-letter ISO 3166-1 alpha-2 code");
			}
			if (!(countryValue instanceof ASN1PrintableString)) {
				violations.add("subject countryName is not encoded as a PrintableString");
			}
		}

		if (subject.getRDNs(BCStyle.CN).length == 0) {
			violations.add("subject does not contain a commonName attribute");
		}

		for (RDN rdn : subject.getRDNs()) {
			ASN1ObjectIdentifier type = rdn.getFirst().getType();
			// DirectoryString-valued attributes used by the Annex B profiles
			if (type.equals(BCStyle.CN) || type.equals(BCStyle.O) || type.equals(BCStyle.OU)
				|| type.equals(BCStyle.L) || type.equals(BCStyle.ST)) {
				ASN1Encodable value = rdn.getFirst().getValue();
				if (!(value instanceof ASN1PrintableString) && !(value instanceof ASN1UTF8String)) {
					violations.add("subject attribute " + BCStyle.INSTANCE.oidToDisplayName(type)
						+ " is not encoded as a PrintableString or UTF8String");
				}
			}
		}
	}

	/**
	 * Checks the subject public key: an allowed EC curve (or, when {@code allowEdwards}, the
	 * Ed25519/Ed448 keys Table B.3 also permits), with EC public keys in uncompressed form.
	 */
	public static void checkSubjectPublicKey(X509Certificate cert, boolean allowEdwards, List<String> violations) {
		SubjectPublicKeyInfo spki = holderOf(cert).getSubjectPublicKeyInfo();
		String algorithm = spki.getAlgorithm().getAlgorithm().getId();
		if (OID_EC_PUBLIC_KEY.equals(algorithm)) {
			ASN1Encodable parameters = spki.getAlgorithm().getParameters();
			String curve = parameters instanceof ASN1ObjectIdentifier oid ? oid.getId() : null;
			if (curve == null || !ALLOWED_EC_CURVES.containsKey(curve)) {
				violations.add("subject public key EC curve " + curve
					+ " is not one of the curves permitted by ISO 18013-5 Annex B");
			}
			byte[] point = spki.getPublicKeyData().getBytes();
			if (point.length == 0 || point[0] != 0x04) {
				violations.add("subject public key is not encoded in uncompressed form");
			}
		} else if (allowEdwards && (OID_ED25519.equals(algorithm) || OID_ED448.equals(algorithm))) {
			// permitted for DS certificates
		} else {
			violations.add("subject public key algorithm " + algorithm
				+ " is not permitted by ISO 18013-5 Annex B");
		}
	}

	/**
	 * Checks that the subject key identifier value is the SHA-1 hash of the subject public key
	 * BIT STRING value, as every Annex B profile requires.
	 */
	public static void checkSubjectKeyIdentifierValue(X509Certificate cert, List<String> violations) {
		byte[] extensionValue = cert.getExtensionValue("2.5.29.14");
		if (extensionValue == null) {
			violations.add("subject key identifier extension is missing");
			return;
		}
		try {
			SubjectKeyIdentifier ski = SubjectKeyIdentifier.getInstance(
				JcaX509ExtensionUtils.parseExtensionValue(extensionValue));
			ASN1BitString publicKeyData = holderOf(cert).getSubjectPublicKeyInfo().getPublicKeyData();
			byte[] expected = MessageDigest.getInstance("SHA-1").digest(publicKeyData.getBytes());
			if (!Arrays.equals(ski.getKeyIdentifier(), expected)) {
				violations.add("subject key identifier is not the SHA-1 hash of the subject public key");
			}
		} catch (Exception e) {
			violations.add("subject key identifier could not be verified: " + e.getMessage());
		}
	}

	/**
	 * Checks the CRL distribution points extension content: at least one distributionPoint
	 * with a URI, and no 'reasons' or 'cRLIssuer' fields ("shall not be used").
	 */
	public static void checkCrlDistributionPointsContent(X509Certificate cert, List<String> violations) {
		byte[] extensionValue = cert.getExtensionValue("2.5.29.31");
		if (extensionValue == null) {
			return; // presence requirements differ per profile and are checked by the caller
		}
		try {
			CRLDistPoint crlDistPoint = CRLDistPoint.getInstance(
				JcaX509ExtensionUtils.parseExtensionValue(extensionValue));
			boolean hasUri = false;
			for (DistributionPoint distributionPoint : crlDistPoint.getDistributionPoints()) {
				if (distributionPoint.getReasons() != null) {
					violations.add("CRL distribution points extension uses the 'reasons' field, which shall not be used");
				}
				if (distributionPoint.getCRLIssuer() != null) {
					violations.add("CRL distribution points extension uses the 'cRLIssuer' field, which shall not be used");
				}
				DistributionPointName name = distributionPoint.getDistributionPoint();
				if (name != null && name.getType() == DistributionPointName.FULL_NAME) {
					for (GeneralName generalName : GeneralNames.getInstance(name.getName()).getNames()) {
						if (generalName.getTagNo() == GeneralName.uniformResourceIdentifier) {
							hasUri = true;
						}
					}
				}
			}
			if (!hasUri) {
				violations.add("CRL distribution points extension does not contain a distribution point URI");
			}
		} catch (Exception e) {
			violations.add("CRL distribution points extension could not be parsed: " + e.getMessage());
		}
	}

	/** Checks that none of the extensions ISO 18013-5 B.1.1 forbids are present. */
	public static void checkForbiddenExtensions(X509Certificate cert, List<String> violations) {
		for (Map.Entry<String, String> forbidden : FORBIDDEN_EXTENSIONS.entrySet()) {
			if (cert.getExtensionValue(forbidden.getKey()) != null) {
				violations.add("the " + forbidden.getValue()
					+ " extension is present; ISO 18013-5 B.1.1 says it shall not be used");
			}
		}
	}
}
