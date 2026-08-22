package net.openid.conformance.condition.client;

import com.nimbusds.jose.util.X509CertUtils;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.MdocCertificateProfileChecks;
import net.openid.conformance.util.MdocUtil;
import org.multipaz.cbor.DataItem;

import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Validates the mdoc document signer certificate against the remaining requirements of the
 * DS certificate profile in ISO/IEC 18013-5 Table B.3 (the key usage requirements are covered
 * by {@link ValidateMdocDsCertificateKeyUsage}): critical mdlDS extended key usage, subject and
 * authority key identifiers, issuer alternative name, CRL distribution points, validity period,
 * serial number size, signature algorithm, subject attributes, and that any further extensions
 * are non-critical.
 */
public class ValidateMdocDsCertificateProfile extends AbstractValidateMdocDsCertificate {

	private static final String OID_KEY_USAGE = "2.5.29.15";
	private static final String OID_CRL_DISTRIBUTION_POINTS = "2.5.29.31";
	private static final String OID_AUTHORITY_KEY_IDENTIFIER = "2.5.29.35";
	private static final String OID_EXTENDED_KEY_USAGE = "2.5.29.37";
	private static final String OID_MDL_DS_EKU = "1.0.18013.5.1.2";
	private static final String MDL_DOCTYPE = "org.iso.18013.5.1.mDL";

	private static final Set<String> ALLOWED_CRITICAL_EXTENSIONS = Set.of(OID_KEY_USAGE, OID_EXTENDED_KEY_USAGE);

	private static final int MAX_VALIDITY_DAYS = 457;

	// GeneralName types in the SEQUENCE returned by getIssuerAlternativeNames()
	private static final int GENERAL_NAME_RFC822 = 1;
	private static final int GENERAL_NAME_URI = 6;

	@Override
	@PreEnvironment(strings = { "mdoc_credential_cbor" })
	public Environment evaluate(Environment env) {
		DataItem issuerSigned = decodeIssuerSigned(env);
		List<X509Certificate> chain = extractDsCertificateChain(issuerSigned);
		X509Certificate dsCert = chain.get(0);
		String subject = dsCert.getSubjectX500Principal().getName();

		List<String> violations = new ArrayList<>();

		MdocCertificateProfileChecks.checkVersionAndSerial(dsCert, violations);
		MdocCertificateProfileChecks.checkValidity(dsCert, MAX_VALIDITY_DAYS, violations);
		MdocCertificateProfileChecks.checkSignatureAlgorithm(dsCert, violations);
		MdocCertificateProfileChecks.checkSubjectAttributes(dsCert, violations);
		MdocCertificateProfileChecks.checkSubjectPublicKey(dsCert, true, violations);
		MdocCertificateProfileChecks.checkSubjectKeyIdentifierValue(dsCert, violations);
		MdocCertificateProfileChecks.checkForbiddenExtensions(dsCert, violations);

		checkExtendedKeyUsage(dsCert, parseMsoDocType(issuerSigned), violations);

		if (dsCert.getExtensionValue(OID_AUTHORITY_KEY_IDENTIFIER) == null) {
			violations.add("authority key identifier extension is missing");
		}
		if (dsCert.getExtensionValue(OID_CRL_DISTRIBUTION_POINTS) == null) {
			violations.add("CRL distribution points extension is missing");
		}
		MdocCertificateProfileChecks.checkCrlDistributionPointsContent(dsCert, violations);

		checkIssuerAlternativeName(dsCert, violations);
		checkIssuerBinding(dsCert, issuingCertificate(chain, env), violations);

		Set<String> criticalOids = dsCert.getCriticalExtensionOIDs();
		if (criticalOids != null) {
			for (String oid : criticalOids) {
				if (!ALLOWED_CRITICAL_EXTENSIONS.contains(oid)) {
					violations.add("extension " + oid + " is marked critical; ISO 18013-5 Table B.3 only permits further extensions when they are non-critical");
				}
			}
		}

		if (!violations.isEmpty()) {
			throw error("The document signer certificate in the mdoc x5chain does not comply with the ISO 18013-5 document signer certificate profile: "
					+ String.join("; ", violations),
				args("subject", subject, "violations", violations));
		}

		logSuccess("Document signer certificate complies with the ISO 18013-5 document signer certificate profile",
			args("subject", subject));
		return env;
	}

	/**
	 * The certificate that issued the DS certificate: the next certificate in the x5chain when
	 * intermediates are included, otherwise the configured trust anchor (the IACA root) when
	 * one is set. Null when neither is available, in which case the binding checks are skipped.
	 */
	private X509Certificate issuingCertificate(List<X509Certificate> chain, Environment env) {
		if (chain.size() > 1) {
			return chain.get(1);
		}
		String trustAnchorPem = env.getString("credential_trust_anchor_pem");
		if (trustAnchorPem != null) {
			return X509CertUtils.parse(trustAnchorPem);
		}
		return null;
	}

	/**
	 * ISO 18013-5 Table B.3: the DS certificate's issuer must be the same exact binary value as
	 * the subject of the IACA certificate, and its authority key identifier must carry the same
	 * value as the IACA certificate's subject key identifier. Checked against the DS
	 * certificate's direct issuer; note PKIX name chaining (the chain condition) uses lenient
	 * X.500 matching, so an encoding difference passes there but is still flagged here.
	 */
	private void checkIssuerBinding(X509Certificate dsCert, X509Certificate issuingCert, List<String> violations) {
		if (issuingCert == null) {
			return;
		}
		if (!java.util.Arrays.equals(
				dsCert.getIssuerX500Principal().getEncoded(),
				issuingCert.getSubjectX500Principal().getEncoded())) {
			violations.add("issuer is not the same exact binary value as the subject of the issuing certificate ('"
				+ issuingCert.getSubjectX500Principal().getName() + "')");
		}
		try {
			byte[] akiValue = dsCert.getExtensionValue(OID_AUTHORITY_KEY_IDENTIFIER);
			byte[] skiValue = issuingCert.getExtensionValue("2.5.29.14");
			if (akiValue != null && skiValue != null) {
				byte[] akiKeyId = AuthorityKeyIdentifier.getInstance(
					JcaX509ExtensionUtils.parseExtensionValue(akiValue)).getKeyIdentifierOctets();
				byte[] skiKeyId = SubjectKeyIdentifier.getInstance(
					JcaX509ExtensionUtils.parseExtensionValue(skiValue)).getKeyIdentifier();
				if (akiKeyId != null && !java.util.Arrays.equals(akiKeyId, skiKeyId)) {
					violations.add("authority key identifier does not match the subject key identifier of the issuing certificate");
				}
			}
		} catch (Exception e) {
			violations.add("authority key identifier could not be compared with the issuing certificate's subject key identifier: " + e.getMessage());
		}
		// Table B.3: stateOrProvinceName is mandatory (with the same value) when the IACA
		// certificate carries it
		org.bouncycastle.asn1.x500.X500Name issuerSubject =
			org.bouncycastle.asn1.x500.X500Name.getInstance(issuingCert.getSubjectX500Principal().getEncoded());
		org.bouncycastle.asn1.x500.RDN[] issuerStateRdns =
			issuerSubject.getRDNs(org.bouncycastle.asn1.x500.style.BCStyle.ST);
		if (issuerStateRdns.length > 0) {
			org.bouncycastle.asn1.x500.X500Name dsSubject =
				org.bouncycastle.asn1.x500.X500Name.getInstance(dsCert.getSubjectX500Principal().getEncoded());
			org.bouncycastle.asn1.x500.RDN[] dsStateRdns =
				dsSubject.getRDNs(org.bouncycastle.asn1.x500.style.BCStyle.ST);
			String issuerState = org.bouncycastle.asn1.x500.style.IETFUtils.valueToString(
				issuerStateRdns[0].getFirst().getValue());
			if (dsStateRdns.length == 0) {
				violations.add("the issuing certificate carries stateOrProvinceName '" + issuerState
					+ "' but the document signer certificate subject has no stateOrProvinceName");
			} else {
				String dsState = org.bouncycastle.asn1.x500.style.IETFUtils.valueToString(
					dsStateRdns[0].getFirst().getValue());
				if (!dsState.equals(issuerState)) {
					violations.add("the document signer certificate stateOrProvinceName '" + dsState
						+ "' does not match the issuing certificate's '" + issuerState + "'");
				}
			}
		}
	}

	private static String parseMsoDocType(DataItem issuerSigned) {
		try {
			return MdocUtil.parseMso(issuerSigned).getDocType();
		} catch (MdocUtil.MdocParseException e) {
			// an unparsable MSO is reported by ValidateMdocIssuerSignedSignature; here it only
			// means the docType-specific mdlDS EKU requirement cannot be applied
			return null;
		}
	}

	private void checkExtendedKeyUsage(X509Certificate dsCert, String msoDocType, List<String> violations) {
		List<String> extendedKeyUsage;
		try {
			extendedKeyUsage = dsCert.getExtendedKeyUsage();
		} catch (CertificateParsingException e) {
			violations.add("extended key usage extension could not be parsed");
			return;
		}
		if (extendedKeyUsage == null) {
			violations.add("extended key usage extension is missing; it must be present, critical, and identify the document signing key purpose");
			return;
		}
		// Table B.3 requires the mdlDS OID for mDL document signer certificates; other doctypes
		// (e.g. EU PID) may use ecosystem-specific key purposes, so only require the extension
		// itself there (cf. the 18013-5 editor's note on generic mdoc EKU identifiers).
		if (MDL_DOCTYPE.equals(msoDocType) && !extendedKeyUsage.contains(OID_MDL_DS_EKU)) {
			violations.add("extended key usage does not contain " + OID_MDL_DS_EKU + " (mdlDS); it contains " + extendedKeyUsage);
		}
		Set<String> criticalOids = dsCert.getCriticalExtensionOIDs();
		if (criticalOids == null || !criticalOids.contains(OID_EXTENDED_KEY_USAGE)) {
			violations.add("extended key usage extension is not marked critical");
		}
	}

	private void checkIssuerAlternativeName(X509Certificate dsCert, List<String> violations) {
		Collection<List<?>> issuerAltNames;
		try {
			issuerAltNames = dsCert.getIssuerAlternativeNames();
		} catch (CertificateParsingException e) {
			violations.add("issuer alternative name extension could not be parsed");
			return;
		}
		if (issuerAltNames == null) {
			violations.add("issuer alternative name extension is missing; it must contain an rfc822Name or uniformResourceIdentifier with issuer contact information");
			return;
		}
		boolean hasContact = issuerAltNames.stream().anyMatch(name -> {
			int type = (Integer) name.get(0);
			return type == GENERAL_NAME_RFC822 || type == GENERAL_NAME_URI;
		});
		if (!hasContact) {
			violations.add("issuer alternative name extension does not contain an rfc822Name or uniformResourceIdentifier");
		}
	}

}
