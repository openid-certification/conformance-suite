package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Validates the key usage related requirements of ISO/IEC 18013-5 Table B.3 on the mdoc
 * document signer certificate: keyUsage must be a critical extension asserting only
 * digitalSignature, and the certificate must not be a CA certificate.
 *
 * This catches document signer certificates that were (mis)issued with a CA profile —
 * see issue #1891.
 */
public class ValidateMdocDsCertificateKeyUsage extends AbstractValidateMdocDsCertificate {

	// RFC 5280 KeyUsage bit names, in bit order
	private static final String[] KEY_USAGE_NAMES = {
		"digitalSignature", "nonRepudiation", "keyEncipherment", "dataEncipherment",
		"keyAgreement", "keyCertSign", "cRLSign", "encipherOnly", "decipherOnly"
	};

	@Override
	@PreEnvironment(strings = { "mdoc_credential_cbor" })
	public Environment evaluate(Environment env) {
		X509Certificate dsCert = extractDsCertificate(decodeIssuerSigned(env));
		String subject = dsCert.getSubjectX500Principal().getName();

		List<String> violations = new ArrayList<>();

		boolean[] keyUsage = dsCert.getKeyUsage();
		if (keyUsage == null) {
			violations.add("keyUsage extension is missing; ISO 18013-5 Table B.3 requires a critical keyUsage extension with only the digitalSignature bit set");
		} else {
			Set<String> criticalOids = dsCert.getCriticalExtensionOIDs();
			if (criticalOids == null || !criticalOids.contains("2.5.29.15")) {
				violations.add("keyUsage extension is not marked critical");
			}
			if (!keyUsage[0]) {
				violations.add("digitalSignature bit is not set in keyUsage");
			}
			for (int i = 1; i < keyUsage.length && i < KEY_USAGE_NAMES.length; i++) {
				if (keyUsage[i]) {
					violations.add(KEY_USAGE_NAMES[i] + " bit is set in keyUsage but must be 0");
				}
			}
		}

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
