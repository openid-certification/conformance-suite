package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.MdocCertificateProfileChecks;
import org.multipaz.crypto.X509CertChain;
import org.multipaz.crypto.X509CertChainJvmKt;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Validates the leaf certificate of the MSO revocation list's x5chain against the MSO revocation
 * list signer certificate profile in ISO/IEC 18013-5 Table B.9, which 12.3.6.3 applies to both
 * revocation mechanisms: an EC key on a permitted curve, a validity period of at most 1187
 * days, a mandatory subject key identifier, a mandatory authority key identifier (matching the
 * issuer's subject key identifier), a critical key usage with digitalSignature as the only bit
 * set, and no other critical extensions.
 *
 * <p>Separate from {@link ValidateMdocRevocationListCwtFormat} so the caller can treat
 * certificate profile conformance at a different severity to the CWT format requirements.
 */
public class ValidateMdocRevocationListSignerCertificateProfile extends AbstractRevocationListCwtCondition {

	/**
	 * Table B.9 permits only key usage to be critical ("further extensions ... marked
	 * non-critical"); a critical extended key usage is reported by
	 * {@link #checkExtendedKeyUsageIsNotCritical} with the 12.3.6.3 wording instead.
	 */
	private static final Set<String> ALLOWED_CRITICAL_EXTENSIONS = Set.of(
		MdocCertificateProfileChecks.OID_KEY_USAGE, MdocCertificateProfileChecks.OID_EXTENDED_KEY_USAGE);

	private static final int MAX_VALIDITY_DAYS = 1187;

	@Override
	@PreEnvironment(strings = { ENV_TOKEN })
	public Environment evaluate(Environment env) {
		ParsedRevocationListCwt parsed = parseRevocationListCwt(env);

		X509CertChain x5chain = requireProtectedX5chain(parsed.coseSign1(),
			"the signer certificate profile cannot be checked");

		List<X509Certificate> chain = X509CertChainJvmKt.getJavaX509Certificates(x5chain);
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

		MdocCertificateProfileChecks.checkDigitalSignatureOnlyKeyUsage(signerCert, "Table B.9", violations);
		checkExtendedKeyUsageIsNotCritical(signerCert, violations);
		checkAuthorityKeyIdentifier(signerCert, chain.size() > 1 ? chain.get(1) : null, violations);
		MdocCertificateProfileChecks.checkNoOtherCriticalExtensions(signerCert, ALLOWED_CRITICAL_EXTENSIONS,
			"Table B.9", violations);

		if (!violations.isEmpty()) {
			throw error("The MSO revocation list signer certificate does not comply with the"
					+ " ISO/IEC 18013-5 Table B.9 certificate profile: " + String.join("; ", violations),
				args("subject", subject, "violations", violations));
		}

		logSuccess("The MSO revocation list signer certificate complies with the ISO/IEC 18013-5"
			+ " Table B.9 certificate profile", args("subject", subject));
		return env;
	}

	/**
	 * The extended key usage is optional; ISO/IEC 18013-5 12.3.6.3 says it is "not recommended
	 * for issuer authority infrastructure to make the extended key usage field critical".
	 */
	private void checkExtendedKeyUsageIsNotCritical(X509Certificate cert, List<String> violations) {
		Set<String> criticalOids = cert.getCriticalExtensionOIDs();
		if (criticalOids != null && criticalOids.contains(MdocCertificateProfileChecks.OID_EXTENDED_KEY_USAGE)) {
			violations.add("the extended key usage extension is marked critical; ISO/IEC 18013-5"
				+ " 12.3.6.3 recommends against making it critical");
		}
	}

	/** Table B.9 makes the authority key identifier mandatory; the match itself is shared. */
	private void checkAuthorityKeyIdentifier(X509Certificate cert, X509Certificate issuingCert,
			List<String> violations) {
		if (cert.getExtensionValue(MdocCertificateProfileChecks.OID_AUTHORITY_KEY_IDENTIFIER) == null) {
			violations.add("the authority key identifier extension is missing; Table B.9 requires it");
			return;
		}
		MdocCertificateProfileChecks.checkAuthorityKeyIdentifierMatchesIssuer(cert, issuingCert, violations);
	}
}
