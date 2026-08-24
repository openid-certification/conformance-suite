package net.openid.conformance.condition.client;

import com.nimbusds.jose.util.X509CertUtils;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.MdocCertificateProfileChecks;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Validates the configured mdoc trust anchor ('Trust anchor PEM' in the 'Credential Issuer'
 * section of the test configuration) against the IACA root certificate profile in
 * ISO/IEC 18013-5 Table B.1 / B.1.2. Although this checks the tester's own configuration
 * rather than the implementation under test, that configuration should mirror the production
 * trust anchor, so profile errors in it are surfaced. Skipped when no trust anchor is
 * configured.
 */
public class ValidateMdocTrustAnchorIacaCertificateProfile extends AbstractCondition {

	private static final int MAX_VALIDITY_DAYS = 20 * 365 + 5; // Table B.1: maximum of 20 years

	// Table B.1 marks basicConstraints and keyUsage critical; further extensions must be non-critical
	private static final Set<String> ALLOWED_CRITICAL_EXTENSIONS = Set.of("2.5.29.19", "2.5.29.15");

	@Override
	public Environment evaluate(Environment env) {
		String trustAnchorPem = env.getString("credential_trust_anchor_pem");
		if (trustAnchorPem == null) {
			logSuccess("No trust anchor is configured; IACA certificate profile check skipped");
			return env;
		}

		X509Certificate anchor = X509CertUtils.parse(trustAnchorPem);
		if (anchor == null) {
			throw error("The configured trust anchor could not be parsed as an X.509 certificate");
		}
		String subject = anchor.getSubjectX500Principal().getName();

		List<String> violations = new ArrayList<>();

		MdocCertificateProfileChecks.checkVersionAndSerial(anchor, violations);
		MdocCertificateProfileChecks.checkValidity(anchor, MAX_VALIDITY_DAYS, violations);
		MdocCertificateProfileChecks.checkSignatureAlgorithm(anchor, violations);
		MdocCertificateProfileChecks.checkSubjectAttributes(anchor, violations);
		// Table B.1 only permits EC keys for the IACA root (no Ed25519/Ed448)
		MdocCertificateProfileChecks.checkSubjectPublicKey(anchor, false, violations);
		MdocCertificateProfileChecks.checkSubjectKeyIdentifierValue(anchor, violations);
		MdocCertificateProfileChecks.checkCrlDistributionPointsContent(anchor, violations);
		MdocCertificateProfileChecks.checkForbiddenExtensions(anchor, violations);

		if (!Arrays.equals(anchor.getSubjectX500Principal().getEncoded(),
				anchor.getIssuerX500Principal().getEncoded())) {
			violations.add("subject is not the same exact binary value as issuer (the IACA root must be self-signed)");
		}
		try {
			anchor.verify(anchor.getPublicKey());
		} catch (Exception e) {
			violations.add("the self-signature could not be verified: " + e.getMessage());
		}

		boolean[] keyUsage = anchor.getKeyUsage();
		if (keyUsage == null) {
			violations.add("keyUsage extension is missing; Table B.1 requires a critical keyUsage with only keyCertSign and cRLSign");
		} else {
			if (!keyUsage[5] || !keyUsage[6]) {
				violations.add("keyUsage must assert keyCertSign and cRLSign");
			}
			for (int i = 0; i < keyUsage.length; i++) {
				if (keyUsage[i] && i != 5 && i != 6) {
					violations.add("keyUsage asserts a bit other than keyCertSign/cRLSign (bit " + i + ")");
				}
			}
			Set<String> criticalOids = anchor.getCriticalExtensionOIDs();
			if (criticalOids == null || !criticalOids.contains("2.5.29.15")) {
				violations.add("keyUsage extension is not marked critical");
			}
		}

		int pathLen = anchor.getBasicConstraints();
		if (pathLen < 0) {
			violations.add("basicConstraints with cA=true is missing");
		} else {
			if (pathLen != 0) {
				violations.add("basicConstraints pathLenConstraint is "
					+ (pathLen == Integer.MAX_VALUE ? "absent" : String.valueOf(pathLen))
					+ " but Table B.1 requires 0");
			}
			Set<String> criticalOids = anchor.getCriticalExtensionOIDs();
			if (criticalOids == null || !criticalOids.contains("2.5.29.19")) {
				violations.add("basicConstraints extension is not marked critical");
			}
		}

		checkIssuerAlternativeName(anchor, violations);

		Set<String> criticalOids = anchor.getCriticalExtensionOIDs();
		if (criticalOids != null) {
			for (String oid : criticalOids) {
				if (!ALLOWED_CRITICAL_EXTENSIONS.contains(oid)) {
					violations.add("extension " + oid + " is marked critical; ISO 18013-5 Table B.1 only permits further extensions when they are non-critical");
				}
			}
		}

		if (!violations.isEmpty()) {
			throw error("The configured trust anchor does not comply with the ISO 18013-5 IACA root certificate profile: "
					+ String.join("; ", violations),
				args("subject", subject, "violations", violations));
		}

		logSuccess("The configured trust anchor complies with the ISO 18013-5 IACA root certificate profile",
			args("subject", subject));
		return env;
	}

	private void checkIssuerAlternativeName(X509Certificate anchor, List<String> violations) {
		try {
			var issuerAltNames = anchor.getIssuerAlternativeNames();
			if (issuerAltNames == null) {
				violations.add("issuer alternative name extension is missing; it must contain an rfc822Name or uniformResourceIdentifier with issuer contact information");
				return;
			}
			boolean hasContact = issuerAltNames.stream().anyMatch(name -> {
				int type = (Integer) name.get(0);
				return type == 1 || type == 6; // rfc822Name / uniformResourceIdentifier
			});
			if (!hasContact) {
				violations.add("issuer alternative name extension does not contain an rfc822Name or uniformResourceIdentifier");
			}
		} catch (Exception e) {
			violations.add("issuer alternative name extension could not be parsed: " + e.getMessage());
		}
	}
}
