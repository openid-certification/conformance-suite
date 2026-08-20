package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.multipaz.cose.CoseSign1;
import org.multipaz.crypto.X509Cert;
import org.multipaz.crypto.X509CertJvmKt;

import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.util.List;

/**
 * Validates the VICAL signer certificate against the profile in ISO/IEC 18013-5 Annex C.1.7.2
 * (Table C.1 essentials): the certificate must be within its validity period, use an elliptic
 * curve key, and carry the mDL VICAL extended key usage OID.
 */
public class ValidateVicalSignerCertificateProfile extends AbstractVicalCondition {

	// id-mdl-kp-mdlVICAL, ISO/IEC 18013-5 Annex C.1.7.2
	public static final String MDL_VICAL_SIGNER_EKU_OID = "1.0.18013.5.1.8";

	@Override
	@PreEnvironment(required = "vical")
	public Environment evaluate(Environment env) {

		CoseSign1 coseSign1 = getVicalCoseSign1(getVicalBytes(env));
		X509Cert leaf = getVicalSignerCertChain(coseSign1, false).getCertificates().get(0);

		X509Certificate signerCert;
		try {
			signerCert = X509CertJvmKt.getJavaX509Certificate(leaf);
		} catch (Exception e) {
			throw error("Failed to parse the VICAL signer certificate as X.509", e);
		}

		String subject = signerCert.getSubjectX500Principal().getName();

		try {
			signerCert.checkValidity();
		} catch (CertificateExpiredException | CertificateNotYetValidException e) {
			throw error("The VICAL signer certificate is outside its validity period", e,
				args("subject", subject,
					"not_before", signerCert.getNotBefore().toString(),
					"not_after", signerCert.getNotAfter().toString()));
		}

		if (!(signerCert.getPublicKey() instanceof ECPublicKey)) {
			throw error("The VICAL signer certificate does not contain an elliptic curve public key",
				args("subject", subject,
					"key_algorithm", signerCert.getPublicKey().getAlgorithm()));
		}

		List<String> eku;
		try {
			eku = signerCert.getExtendedKeyUsage();
		} catch (Exception e) {
			throw error("Failed to parse the extended key usage extension of the VICAL signer certificate", e,
				args("subject", subject));
		}
		if (eku == null || !eku.contains(MDL_VICAL_SIGNER_EKU_OID)) {
			throw error("The VICAL signer certificate does not contain the mDL VICAL extended key usage",
				args("subject", subject,
					"expected_eku_oid", MDL_VICAL_SIGNER_EKU_OID,
					"extended_key_usage", eku == null ? null : eku.toString()));
		}

		logSuccess("VICAL signer certificate matches the Annex C.1.7.2 profile",
			args("subject", subject,
				"not_before", signerCert.getNotBefore().toString(),
				"not_after", signerCert.getNotAfter().toString()));

		return env;
	}
}
