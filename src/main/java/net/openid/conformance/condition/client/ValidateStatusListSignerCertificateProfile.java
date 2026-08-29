package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.MdocCertificateProfileChecks;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.multipaz.cose.CoseSign1;
import org.multipaz.crypto.X509Cert;
import org.multipaz.crypto.X509CertChain;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Validates the leaf certificate of the MSO revocation list's x5chain against the MSO revocation
 * list signer certificate profile in ISO/IEC 18013-5 Table B.9: an EC key on a permitted curve,
 * a validity period of at most 1187 days, a mandatory subject key identifier, a mandatory
 * authority key identifier (matching the issuer's subject key identifier), a critical key usage
 * with digitalSignature as the only bit set, and no other critical extensions.
 *
 * <p>Separate from {@link ValidateStatusListTokenCwtFormat} so the caller can treat certificate
 * profile conformance at a different severity to the CWT format requirements.
 */
public class ValidateStatusListSignerCertificateProfile extends AbstractStatusListCwtCondition {

	private static final String OID_KEY_USAGE = "2.5.29.15";
	private static final String OID_SUBJECT_KEY_IDENTIFIER = "2.5.29.14";
	private static final String OID_AUTHORITY_KEY_IDENTIFIER = "2.5.29.35";
	private static final String OID_EXTENDED_KEY_USAGE = "2.5.29.37";

	/** Table B.9 permits only key usage to be critical ("further extensions ... marked non-critical"). */
	private static final Set<String> ALLOWED_CRITICAL_EXTENSIONS = Set.of(OID_KEY_USAGE);

	private static final int MAX_VALIDITY_DAYS = 1187;

	// KeyUsage bit positions, in the order X509Certificate.getKeyUsage() returns them
	private static final String[] KEY_USAGE_NAMES = {
		"digitalSignature", "nonRepudiation", "keyEncipherment", "dataEncipherment",
		"keyAgreement", "keyCertSign", "cRLSign", "encipherOnly", "decipherOnly"
	};

	@Override
	@PreEnvironment(strings = { ENV_STATUS_LIST_TOKEN })
	public Environment evaluate(Environment env) {
		ParsedStatusListCwt parsed = parseStatusListCwt(env);
		CoseSign1 coseSign1 = parsed.coseSign1();

		X509CertChain x5chain = getProtectedX5chain(coseSign1);
		if (x5chain == null || x5chain.getCertificates().isEmpty()) {
			throw error("The MSO revocation list does not contain an x5chain in its protected header,"
				+ " so the signer certificate profile cannot be checked");
		}

		List<X509Certificate> chain = parseChain(x5chain);
		X509Certificate signerCert = chain.get(0);
		String subject = signerCert.getSubjectX500Principal().getName();

		List<String> violations = new ArrayList<>();

		MdocCertificateProfileChecks.checkVersionAndSerial(signerCert, violations);
		MdocCertificateProfileChecks.checkValidity(signerCert, MAX_VALIDITY_DAYS, violations);
		MdocCertificateProfileChecks.checkSignatureAlgorithm(signerCert, violations);
		// Table B.9 permits only EC keys, unlike the document signer profile which also allows
		// the Edwards curves
		MdocCertificateProfileChecks.checkSubjectPublicKey(signerCert, false, violations);
		MdocCertificateProfileChecks.checkSubjectKeyIdentifierValue(signerCert, violations);
		MdocCertificateProfileChecks.checkCrlDistributionPointsContent(signerCert, violations);
		MdocCertificateProfileChecks.checkForbiddenExtensions(signerCert, violations);

		checkKeyUsage(signerCert, violations);
		checkExtendedKeyUsageIsNotCritical(signerCert, violations);
		checkAuthorityKeyIdentifier(signerCert, chain.size() > 1 ? chain.get(1) : null, violations);
		checkCriticalExtensions(signerCert, violations);

		if (!violations.isEmpty()) {
			throw error("The MSO revocation list signer certificate does not comply with the"
					+ " ISO/IEC 18013-5 Table B.9 certificate profile: " + String.join("; ", violations),
				args("subject", subject, "violations", violations));
		}

		logSuccess("The MSO revocation list signer certificate complies with the ISO/IEC 18013-5"
			+ " Table B.9 certificate profile", args("subject", subject));
		return env;
	}

	private List<X509Certificate> parseChain(X509CertChain x5chain) {
		try {
			CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
			List<X509Certificate> certs = new ArrayList<>();
			for (X509Cert cert : x5chain.getCertificates()) {
				var encoded = cert.getEncoded();
				certs.add((X509Certificate) certificateFactory.generateCertificate(
					new ByteArrayInputStream(encoded.toByteArray(0, encoded.getSize()))));
			}
			return certs;
		} catch (Exception e) {
			throw error("Failed to parse a certificate in the MSO revocation list's x5chain as an"
				+ " X.509 certificate", e);
		}
	}

	private void checkKeyUsage(X509Certificate cert, List<String> violations) {
		boolean[] keyUsage = cert.getKeyUsage();
		if (keyUsage == null) {
			violations.add("the key usage extension is missing; Table B.9 requires it to be present,"
				+ " critical, and to assert digitalSignature only");
			return;
		}
		List<String> asserted = new ArrayList<>();
		for (int i = 0; i < KEY_USAGE_NAMES.length && i < keyUsage.length; i++) {
			if (keyUsage[i]) {
				asserted.add(KEY_USAGE_NAMES[i]);
			}
		}
		if (!List.of("digitalSignature").equals(asserted)) {
			violations.add("the key usage extension asserts " + asserted
				+ " but Table B.9 requires digitalSignature to be the only bit set");
		}
		Set<String> criticalOids = cert.getCriticalExtensionOIDs();
		if (criticalOids == null || !criticalOids.contains(OID_KEY_USAGE)) {
			violations.add("the key usage extension is not marked critical");
		}
	}

	/**
	 * The extended key usage is optional; ISO/IEC 18013-5 12.3.6.3 says it is "not recommended
	 * for issuer authority infrastructure to make the extended key usage field critical".
	 */
	private void checkExtendedKeyUsageIsNotCritical(X509Certificate cert, List<String> violations) {
		List<String> extendedKeyUsage;
		try {
			extendedKeyUsage = cert.getExtendedKeyUsage();
		} catch (CertificateParsingException e) {
			violations.add("the extended key usage extension could not be parsed");
			return;
		}
		if (extendedKeyUsage == null) {
			return;
		}
		Set<String> criticalOids = cert.getCriticalExtensionOIDs();
		if (criticalOids != null && criticalOids.contains(OID_EXTENDED_KEY_USAGE)) {
			violations.add("the extended key usage extension is marked critical; ISO/IEC 18013-5"
				+ " 12.3.6.3 recommends against making it critical");
		}
	}

	private void checkAuthorityKeyIdentifier(X509Certificate cert, X509Certificate issuingCert,
			List<String> violations) {
		byte[] akiValue = cert.getExtensionValue(OID_AUTHORITY_KEY_IDENTIFIER);
		if (akiValue == null) {
			violations.add("the authority key identifier extension is missing; Table B.9 requires it");
			return;
		}
		if (issuingCert == null) {
			// nothing to compare against when the chain only contains the signer certificate
			return;
		}
		try {
			byte[] skiValue = issuingCert.getExtensionValue(OID_SUBJECT_KEY_IDENTIFIER);
			if (skiValue == null) {
				return;
			}
			byte[] akiKeyId = AuthorityKeyIdentifier.getInstance(
				JcaX509ExtensionUtils.parseExtensionValue(akiValue)).getKeyIdentifierOctets();
			byte[] skiKeyId = SubjectKeyIdentifier.getInstance(
				JcaX509ExtensionUtils.parseExtensionValue(skiValue)).getKeyIdentifier();
			if (akiKeyId != null && !Arrays.equals(akiKeyId, skiKeyId)) {
				violations.add("the authority key identifier does not match the subject key identifier"
					+ " of the issuing certificate");
			}
		} catch (Exception e) {
			violations.add("the authority key identifier could not be compared with the issuing"
				+ " certificate's subject key identifier: " + e.getMessage());
		}
	}

	private void checkCriticalExtensions(X509Certificate cert, List<String> violations) {
		Set<String> criticalOids = cert.getCriticalExtensionOIDs();
		if (criticalOids == null) {
			return;
		}
		for (String oid : criticalOids) {
			if (!ALLOWED_CRITICAL_EXTENSIONS.contains(oid) && !OID_EXTENDED_KEY_USAGE.equals(oid)) {
				violations.add("extension " + oid + " is marked critical; Table B.9 only permits"
					+ " further extensions when they are non-critical");
			}
		}
	}
}
