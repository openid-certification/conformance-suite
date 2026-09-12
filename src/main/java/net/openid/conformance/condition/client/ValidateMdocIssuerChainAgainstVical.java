package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.multipaz.cbor.Cbor;
import org.multipaz.cbor.DataItem;
import net.openid.conformance.util.MdocUtil;
import org.multipaz.crypto.X509Cert;
import org.multipaz.crypto.X509CertChain;
import org.multipaz.mdoc.vical.SignedVical;
import org.multipaz.mdoc.vical.VicalCertificateInfo;
import org.multipaz.trustmanagement.TrustResult;
import org.multipaz.trustmanagement.VicalTrustManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Validates that the x5chain of the mdoc credential's issuerAuth chains to an IACA certificate
 * listed in the configured VICAL, and that the VICAL entry lists the credential's document type
 * (ISO/IEC 18013-5 Annex C.1.7.1: relying parties should not use a certificate as a trust point
 * for a document type not listed in the entry's docType).
 */
public class ValidateMdocIssuerChainAgainstVical extends AbstractVicalCondition {

	@Override
	@PreEnvironment(required = "vical", strings = { "mdoc_credential_cbor" })
	public Environment evaluate(Environment env) {

		DataItem issuerSigned;
		try {
			issuerSigned = Cbor.INSTANCE.decode(Base64.getDecoder().decode(env.getString("mdoc_credential_cbor")));
		} catch (Exception e) {
			throw error("Failed to parse the mdoc IssuerSigned structure", e);
		}

		X509CertChain certChain;
		try {
			certChain = MdocUtil.extractX5chain(issuerSigned);
		} catch (Exception e) {
			throw error("Failed to extract the x5chain from the mdoc issuerAuth", e);
		}

		String docType;
		try {
			docType = MdocUtil.parseMso(issuerSigned).getDocType();
		} catch (Exception e) {
			throw error("Failed to extract the docType from the mdoc Mobile Security Object", e);
		}

		// Verify the signature here (against the VICAL's embedded x5chain) so a corrupted
		// VICAL cannot silently drive the trust verdict; there is no VICAL provider trust
		// anchor in this design, so this is an integrity check, not an authenticity check.
		// This deliberately errors at the caller's severity (FAILURE for issuance) rather
		// than skipping: the tester explicitly configured this check, and a silent skip
		// would look like a pass - see the severity policy in SetupVicalFromConfiguration.
		SignedVical signedVical;
		try {
			signedVical = parseSignedVical(getVicalBytes(env), false);
		} catch (Exception e) {
			throw error("The configured VICAL could not be parsed or its COSE signature does not verify, so the mdoc issuer certificate chain cannot be evaluated against it", e);
		}

		String issuerCertSubject = certChain.getCertificates().get(0).getSubject().getName();
		String vicalProvider = signedVical.getVical().getVicalProvider();

		TrustResult trustResult;
		try {
			VicalTrustManager trustManager = new VicalTrustManager(signedVical, "vical");
			List<X509Cert> chainCerts = certChain.getCertificates();
			kotlin.time.Instant now = kotlin.time.Instant.Companion.fromEpochMilliseconds(System.currentTimeMillis());
			trustResult = kotlinx.coroutines.BuildersKt.runBlocking(
				kotlin.coroutines.EmptyCoroutineContext.INSTANCE,
				// true = also validate the validity intervals of CA certificates in the chain
				(scope, continuation) -> trustManager.verify(chainCerts, now, true, continuation)
			);
		} catch (Exception e) {
			throw error("Failed to evaluate the mdoc issuer certificate chain against the VICAL", e);
		}

		if (!trustResult.isTrusted()) {
			throw error("The mdoc issuer certificate chain does not chain to any IACA certificate in the configured VICAL",
				args("issuer_certificate_subject", issuerCertSubject,
					"vical_provider", vicalProvider,
					"error", trustResult.getError() == null ? null : trustResult.getError().getMessage()));
		}

		X509Cert trustPoint = trustResult.getTrustPoints().isEmpty()
			? null : trustResult.getTrustPoints().get(0).getCertificate();
		// the same IACA may be listed in several entries with different docTypes, so
		// union the docTypes across all entries matching the trust point
		List<VicalCertificateInfo> matchedEntries = findVicalEntries(signedVical, trustPoint);
		if (matchedEntries.isEmpty()) {
			// shouldn't happen: the trust point came from the VICAL
			throw error("Could not locate the matched IACA certificate in the VICAL",
				args("issuer_certificate_subject", issuerCertSubject));
		}

		Set<String> listedDocTypes = new LinkedHashSet<>();
		matchedEntries.forEach(entry -> listedDocTypes.addAll(entry.getDocTypes()));
		VicalCertificateInfo firstEntry = matchedEntries.get(0);

		if (!listedDocTypes.contains(docType)) {
			throw error("The VICAL entries for the matched IACA certificate do not list the credential's document type, so the certificate should not be used as a trust point for this credential",
				args("doc_type", docType,
					"vical_entry_doc_types", listedDocTypes.toString(),
					"iaca_certificate_subject", firstEntry.getCertificate().getSubject().getName(),
					"vical_provider", vicalProvider));
		}

		logSuccess("The mdoc issuer certificate chains to an IACA certificate listed in the VICAL for this document type",
			args("doc_type", docType,
				"issuer_certificate_subject", issuerCertSubject,
				"iaca_certificate_subject", firstEntry.getCertificate().getSubject().getName(),
				"issuing_authority", firstEntry.getIssuingAuthority(),
				"vical_provider", vicalProvider,
				// the VICAL's own validity is checked (as a WARNING) by ValidateVicalStructure;
				// recorded here so the basis of the trust verdict is visible in this entry too
				"vical_date", signedVical.getVical().getDate().toString(),
				"vical_not_after", signedVical.getVical().getNotAfter() == null
					? null : signedVical.getVical().getNotAfter().toString()));

		return env;
	}

	private List<VicalCertificateInfo> findVicalEntries(SignedVical signedVical, X509Cert trustPoint) {
		List<VicalCertificateInfo> matches = new ArrayList<>();
		if (trustPoint == null) {
			return matches;
		}
		byte[] trustPointSki = trustPoint.getSubjectKeyIdentifier();
		for (VicalCertificateInfo certInfo : signedVical.getVical().getCertificateInfos()) {
			// match by certificate or SKI (a renewed IACA keeping its key has a new certificate)
			if (certInfo.getCertificate().equals(trustPoint)
				|| (trustPointSki != null
					&& Arrays.equals(certInfo.getCertificate().getSubjectKeyIdentifier(), trustPointSki))) {
				matches.add(certInfo);
			}
		}
		return matches;
	}
}
