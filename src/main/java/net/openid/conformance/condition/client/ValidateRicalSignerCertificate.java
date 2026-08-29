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

/**
 * Validates the RICAL signer certificate as far as ISO/IEC 18013-5 second edition draft
 * Annex F.3.2.5 allows without the out-of-band RICAL root: the certificate must be within its
 * validity period and use an elliptic curve key (matching the ES256/ES384/ES512 signing
 * algorithms the annex names). Unlike the VICAL (Annex C.1.7.2), Annex F defines no signer
 * certificate profile or extended key usage, so no EKU is required. Full RFC 5280 path
 * validation to the RICAL root is out of scope: the root is distributed out of band and is not
 * part of the test configuration.
 */
public class ValidateRicalSignerCertificate extends AbstractRicalCondition {

	@Override
	@PreEnvironment(required = "rical")
	public Environment evaluate(Environment env) {

		CoseSign1 coseSign1 = getRicalCoseSign1(getRicalBytes(env));
		X509Cert leaf = getRicalSignerCertChain(coseSign1, false).getCertificates().get(0);

		X509Certificate signerCert;
		try {
			signerCert = X509CertJvmKt.getJavaX509Certificate(leaf);
		} catch (Exception e) {
			throw error("Failed to parse the RICAL signer certificate as X.509", e);
		}

		String subject = signerCert.getSubjectX500Principal().getName();

		try {
			signerCert.checkValidity();
		} catch (CertificateExpiredException | CertificateNotYetValidException e) {
			throw error("The RICAL signer certificate is outside its validity period", e,
				args("subject", subject,
					"not_before", signerCert.getNotBefore().toString(),
					"not_after", signerCert.getNotAfter().toString()));
		}

		if (!(signerCert.getPublicKey() instanceof ECPublicKey)) {
			throw error("The RICAL signer certificate does not contain an elliptic curve public key",
				args("subject", subject,
					"key_algorithm", signerCert.getPublicKey().getAlgorithm()));
		}

		logSuccess("RICAL signer certificate is within its validity period and uses an EC key",
			args("subject", subject,
				"not_before", signerCert.getNotBefore().toString(),
				"not_after", signerCert.getNotAfter().toString()));

		return env;
	}
}
