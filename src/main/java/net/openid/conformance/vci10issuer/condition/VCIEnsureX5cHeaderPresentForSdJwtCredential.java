package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import com.nimbusds.jose.util.X509CertUtils;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.X509CertificateUtil;

import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;

/**
 * Validates that an SD-JWT credential has a valid x5c header claim.
 *
 * Per HAIP section 4.5.1:
 * - The x5c header MUST be present
 * - The leaf certificate MUST NOT be self-signed
 * - The trust anchor MUST NOT be included in the x5c chain
 */
public class VCIEnsureX5cHeaderPresentForSdJwtCredential extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"sdjwt"})
	public Environment evaluate(Environment env) {

		JsonObject credentialHeader = env.getElementFromObject("sdjwt", "credential.header").getAsJsonObject();

		if (!credentialHeader.has("x5c")) {
			throw error("Credential MUST contain an x5c in the header", args("credential_header", credentialHeader));
		}

		List<String> x5c = OIDFJSON.convertJsonArrayToList(credentialHeader.getAsJsonArray("x5c"));

		if (x5c.isEmpty()) {
			throw error("x5c header claim MUST NOT be empty", args("credential_header", credentialHeader));
		}

		// Parse the leaf certificate (first in the chain)
		String encodedLeafCert = x5c.get(0);
		byte[] leafCertDer = Base64.getDecoder().decode(encodedLeafCert);
		X509Certificate leafCert = X509CertUtils.parse(leafCertDer);

		if (leafCert == null) {
			throw error("Failed to parse leaf certificate from x5c header",
				args("x5c", x5c, "leaf_cert_encoded", encodedLeafCert));
		}

		// Per HAIP section 4.5.1: Credential signing certificate must NOT be self-signed
		if (X509CertificateUtil.isSelfSigned(leafCert)) {
			throw error("Credential signing certificate MUST NOT be self-signed (HAIP section 4.5.1)",
				args("x5c", x5c, "leaf_cert_subject", leafCert.getSubjectX500Principal().getName()));
		}

		// Per HAIP section 4.5.1: The trust anchor MUST NOT be included in the x5c chain
		// The trust anchor is a self-signed root CA certificate. If the last certificate in the
		// chain is self-signed, it is a trust anchor and should not be included.
		if (x5c.size() > 1) {
			String encodedLastCert = x5c.get(x5c.size() - 1);
			byte[] lastCertDer = Base64.getDecoder().decode(encodedLastCert);
			X509Certificate lastCert = X509CertUtils.parse(lastCertDer);

			if (lastCert != null && X509CertificateUtil.isSelfSigned(lastCert)) {
				throw error("The trust anchor (self-signed root CA) MUST NOT be included in the x5c chain (HAIP section 4.5.1). " +
					"The last certificate in the x5c array is self-signed, indicating it is the trust anchor.",
					args("x5c", x5c,
						"trust_anchor_subject", lastCert.getSubjectX500Principal().getName(),
						"chain_length", x5c.size()));
			}
		}

		log("Credential signing certificate is not self-signed and trust anchor is not included in chain (as required)",
			args("leaf_cert_subject", leafCert.getSubjectX500Principal().getName(),
				"chain_length", x5c.size()));

		logSuccess("Found valid credential x5c claim in header", args("x5c", x5c));

		return env;
	}

}
