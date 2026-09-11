package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.MdocCertificateProfileChecks;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates the key usage related requirements of ISO/IEC 18013-5 Table B.3 on the mdoc
 * document signer certificate: keyUsage must be a critical extension asserting only
 * digitalSignature, and the certificate must not be a CA certificate.
 *
 * This catches document signer certificates that were (mis)issued with a CA profile —
 * see issue #1891.
 */
public class ValidateMdocDsCertificateKeyUsage extends AbstractValidateMdocDsCertificate {

	@Override
	@PreEnvironment(strings = { "mdoc_credential_cbor" })
	public Environment evaluate(Environment env) {
		X509Certificate dsCert = extractDsCertificate(decodeIssuerSigned(env));
		String subject = dsCert.getSubjectX500Principal().getName();

		List<String> violations = new ArrayList<>();

		MdocCertificateProfileChecks.checkDigitalSignatureOnlyKeyUsage(dsCert, "Table B.3", violations);

		if (dsCert.getBasicConstraints() >= 0) {
			violations.add("certificate has basicConstraints with cA=true; the document signer certificate must not be a CA certificate");
		}

		if (!violations.isEmpty()) {
			throw error("The document signer certificate in the mdoc x5chain does not meet the key usage requirements of the ISO 18013-5 document signer certificate profile (strict verifiers will reject credentials signed with it): "
					+ String.join("; ", violations),
				args("subject", subject, "violations", violations));
		}

		logSuccess("Document signer certificate has digitalSignature-only keyUsage and is not a CA certificate",
			args("subject", subject));
		return env;
	}
}
