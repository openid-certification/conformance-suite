package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.MdocCertificateProfileChecks;
import net.openid.conformance.util.X509CertificateUtil;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x509.TBSCertList;
import org.bouncycastle.cert.X509CRLHolder;

import java.math.BigInteger;
import java.security.cert.X509CRL;
import java.security.cert.X509CRLEntry;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Validates the CRL fetched for an mdoc's document signer certificate against the CRL profile in
 * ISO/IEC 18013-5 Table B.10 and the issuance rules of B.2: version 2, issuer equal to the
 * document signer certificate's issuer (B.2 allows neither indirect nor delta CRLs), ECDSA with
 * SHA-256, SHA-384 or SHA-512 and the same algorithm inside and outside the signature,
 * thisUpdate not in the future and nextUpdate present and not yet passed (Table B.10: the next
 * CRL is issued no later than nextUpdate; the 90-day bound of B.2 is
 * {@link EnsureMdocDsCertificateCrlNextUpdateWithin90Days}), revoked
 * entries without CRL entry
 * extensions, a non-critical authority key identifier equal to the document signer
 * certificate's and to the issuing IACA's subject key identifier, a non-critical CRL number that
 * is a non-negative integer of at most 20 octets (RFC 5280 5.2.3), and
 * no further extensions. The signature is verified with the issuing IACA certificate when one
 * can be located (the next x5chain certificate, the matching VICAL entry or the configured trust
 * anchor); otherwise that one check is logged as not made. Only a compliant CRL whose signature
 * verified is marked as authenticated, the precondition for reading revocation status from it.
 */
public class ValidateMdocDsCertificateCrl extends AbstractMdocDsCertificateCrlCondition {

	private static final Set<String> REQUIRED_NON_CRITICAL_EXTENSIONS = Set.of(
		MdocCertificateProfileChecks.OID_AUTHORITY_KEY_IDENTIFIER, MdocCertificateProfileChecks.OID_CRL_NUMBER);

	/** As for the MSO validity period: a CRL is minted at second precision by a clock that is not ours. */
	private static final Duration ALLOWED_CLOCK_SKEW = Duration.ofMinutes(5);

	/** RFC 5280 5.2.3: conforming CRL issuers MUST NOT use CRLNumber values longer than 20 octets. */
	private static final int MAX_CRL_NUMBER_OCTETS = 20;

	@Override
	@PreEnvironment(strings = { "mdoc_credential_cbor", ENV_CRL })
	public Environment evaluate(Environment env) {
		List<X509Certificate> chain = extractDsCertificateChain(decodeIssuerSigned(env));
		X509Certificate dsCert = chain.get(0);
		X509CRL crl = parseCrl(env);
		X509Certificate issuingCert = issuingCertificate(chain, env);

		List<String> violations = new ArrayList<>();

		if (crl.getVersion() != 2) {
			violations.add("version is " + crl.getVersion() + " but shall be v2");
		}
		if (!Arrays.equals(crl.getIssuerX500Principal().getEncoded(), dsCert.getIssuerX500Principal().getEncoded())) {
			violations.add("issuer '" + crl.getIssuerX500Principal().getName()
				+ "' is not the same exact binary value as the document signer certificate's issuer '"
				+ dsCert.getIssuerX500Principal().getName() + "'");
		}
		TBSCertList tbsCertList = null;
		try {
			tbsCertList = new X509CRLHolder(crl.getEncoded()).toASN1Structure().getTBSCertList();
		} catch (Exception e) {
			violations.add("the TBS certificate list could not be decoded: " + e.getMessage());
		}
		checkSignatureAlgorithm(crl, tbsCertList, violations);
		checkUpdateTimes(crl, violations);
		checkRevokedEntries(crl, tbsCertList, violations);
		checkExtensions(crl, dsCert, issuingCert, violations);
		if (issuingCert == null) {
			log("The issuing IACA certificate could not be located (it is not in the x5chain, and neither a VICAL entry nor the"
				+ " configured trust anchor issued the document signer certificate), so the CRL signature was not verified");
		} else {
			try {
				crl.verify(issuingCert.getPublicKey());
			} catch (Exception e) {
				violations.add("signature does not verify with the issuing IACA certificate's public key ("
					+ e.getMessage() + ")");
			}
		}

		Set<? extends X509CRLEntry> revoked = crl.getRevokedCertificates();
		Map<String, Object> details = args("uri", env.getString(ENV_URI),
			"crl_issuer", crl.getIssuerX500Principal().getName(),
			"this_update", crl.getThisUpdate(), "next_update", crl.getNextUpdate(),
			"revoked_count", revoked == null ? 0 : revoked.size(),
			"signature_verified", issuingCert != null);
		if (!violations.isEmpty()) {
			details.put("violations", violations);
			details.put("issuing_certificate_pem", issuingCert == null ? null : X509CertificateUtil.toPem(issuingCert));
			throw error("The CRL named by the document signer certificate does not comply with the ISO 18013-5 CRL profile: "
				+ String.join("; ", violations), details);
		}

		if (issuingCert != null) {
			env.putString(ENV_AUTHENTICATED, "true");
		}
		logSuccess("The CRL named by the document signer certificate complies with the ISO 18013-5 CRL profile", details);
		return env;
	}

	private void checkSignatureAlgorithm(X509CRL crl, TBSCertList tbsCertList, List<String> violations) {
		String outer = crl.getSigAlgOID();
		if (!MdocCertificateProfileChecks.ECDSA_SIGNATURE_ALGORITHM_OIDS.contains(outer)) {
			violations.add("signature algorithm " + outer + " is not ECDSA with SHA-256, SHA-384 or SHA-512");
		}
		if (tbsCertList == null) {
			return;
		}
		String inner = tbsCertList.getSignature().getAlgorithm().getId();
		if (!inner.equals(outer)) {
			violations.add("the signature algorithm in the TBS certificate list (" + inner
				+ ") differs from the signature algorithm of the CRL (" + outer + ")");
		}
	}

	private void checkUpdateTimes(X509CRL crl, List<String> violations) {
		Instant now = Instant.now();
		if (crl.getThisUpdate().toInstant().isAfter(now.plus(ALLOWED_CLOCK_SKEW))) {
			violations.add("thisUpdate " + crl.getThisUpdate() + " is in the future");
		}
		if (crl.getNextUpdate() == null) {
			violations.add("nextUpdate is missing");
		} else if (crl.getNextUpdate().toInstant().isBefore(now.minus(ALLOWED_CLOCK_SKEW))) {
			violations.add("nextUpdate " + crl.getNextUpdate() + " has passed, so the IACA has not issued the fresh CRL B.2 requires");
		}
	}

	private void checkRevokedEntries(X509CRL crl, TBSCertList tbsCertList, List<String> violations) {
		Set<? extends X509CRLEntry> entries = crl.getRevokedCertificates();
		// the JCA CRL reports an empty list as absent, so presence is read from the DER structure
		if ((entries == null || entries.isEmpty()) && tbsCertList != null && hasRevokedCertificatesList(tbsCertList)) {
			violations.add("revokedCertificates is present but empty; it shall not be present when nothing is revoked");
		}
		if (entries == null) {
			return;
		}
		for (X509CRLEntry entry : entries) {
			if (entry.hasExtensions()) {
				violations.add("the entry for serial number " + entry.getSerialNumber()
					+ " carries CRL entry extensions, which shall not be used");
				break;
			}
		}
	}

	/**
	 * Whether the TBSCertList (RFC 5280 5.1) carries a revokedCertificates SEQUENCE, empty or not:
	 * after signature, issuer and thisUpdate it is the one untagged SEQUENCE that can follow.
	 */
	private static boolean hasRevokedCertificatesList(TBSCertList tbsCertList) {
		ASN1Sequence fields = ASN1Sequence.getInstance(tbsCertList.toASN1Primitive());
		int index = fields.getObjectAt(0) instanceof ASN1Integer ? 4 : 3;
		for (; index < fields.size(); index++) {
			if (fields.getObjectAt(index) instanceof ASN1Sequence) {
				return true;
			}
		}
		return false;
	}

	/** RFC 5280 5.2.3, which Table B.10 cites: the CRL number is a non-negative integer of at most 20 octets. */
	private static void checkCrlNumber(byte[] extensionValue, List<String> violations) {
		BigInteger number;
		try {
			number = ASN1Integer.getInstance(ASN1OctetString.getInstance(extensionValue).getOctets()).getValue();
		} catch (Exception e) {
			violations.add("CRL number extension does not contain a DER encoded INTEGER (" + e.getMessage() + ")");
			return;
		}
		if (number.signum() < 0) {
			violations.add("CRL number " + number + " is negative, but RFC 5280 section 5.2.3 requires a non-negative integer");
		} else if (number.toByteArray().length > MAX_CRL_NUMBER_OCTETS) {
			violations.add("CRL number is " + number.toByteArray().length + " octets long, but RFC 5280 section 5.2.3 forbids"
				+ " CRL issuers from using values longer than " + MAX_CRL_NUMBER_OCTETS + " octets");
		}
	}

	private void checkExtensions(X509CRL crl, X509Certificate dsCert, X509Certificate issuingCert, List<String> violations) {
		Set<String> critical = crl.getCriticalExtensionOIDs();
		if (critical != null && !critical.isEmpty()) {
			violations.add("critical extensions " + critical + " are present but no critical extension is allowed");
		}
		Set<String> nonCritical = crl.getNonCriticalExtensionOIDs() == null
			? Set.of() : new HashSet<>(crl.getNonCriticalExtensionOIDs());
		if (!nonCritical.contains(MdocCertificateProfileChecks.OID_AUTHORITY_KEY_IDENTIFIER)) {
			violations.add("authority key identifier extension is missing");
		}
		byte[] crlNumberExtension = crl.getExtensionValue(MdocCertificateProfileChecks.OID_CRL_NUMBER);
		if (crlNumberExtension == null) {
			violations.add("CRL number extension is missing");
		} else {
			checkCrlNumber(crlNumberExtension, violations);
		}
		Set<String> further = new HashSet<>(nonCritical);
		further.removeAll(REQUIRED_NON_CRITICAL_EXTENSIONS);
		if (!further.isEmpty()) {
			violations.add("further extensions " + further + " are present but shall not be");
		}

		byte[] crlAkiExtension = crl.getExtensionValue(MdocCertificateProfileChecks.OID_AUTHORITY_KEY_IDENTIFIER);
		if (crlAkiExtension == null) {
			return;
		}
		byte[] crlAki = MdocCertificateProfileChecks.authorityKeyIdentifier(crl);
		if (crlAki == null) {
			violations.add("authority key identifier extension carries no keyIdentifier (Table B.10 requires the"
				+ " keyIdentifier form, not authorityCertIssuer and authorityCertSerialNumber)");
			return;
		}
		byte[] dsAki = MdocCertificateProfileChecks.authorityKeyIdentifier(dsCert);
		if (dsAki != null && !Arrays.equals(crlAki, dsAki)) {
			violations.add("authority key identifier does not match the document signer certificate's authority key identifier");
		}
		MdocCertificateProfileChecks.checkAuthorityKeyIdentifierMatchesIssuer(crl, issuingCert, violations);
	}
}
