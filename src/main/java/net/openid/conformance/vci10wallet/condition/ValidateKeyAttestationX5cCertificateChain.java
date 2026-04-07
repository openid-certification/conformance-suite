package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nimbusds.jose.util.X509CertUtils;
import net.openid.conformance.condition.AbstractValidateX5cCertificateChain;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.security.cert.X509Certificate;
import java.util.List;

/**
 * Validates the x5c certificate chain in a key attestation JWT.
 *
 * Per HAIP section 4.5.1:
 * - The leaf certificate MUST NOT be self-signed
 * - The trust anchor MUST NOT be included in the x5c chain
 * - If a trust anchor is configured, the chain must be verifiable against it
 */
public class ValidateKeyAttestationX5cCertificateChain extends AbstractValidateX5cCertificateChain {

	@Override
	@PreEnvironment(required = {"vci"})
	public Environment evaluate(Environment env) {

		JsonObject keyAttestationJwt = env.getElementFromObject("vci", "key_attestation_jwt").getAsJsonObject();
		JsonObject header = keyAttestationJwt.getAsJsonObject("header");

		JsonElement x5cEl = header.get("x5c");
		if (x5cEl == null || !x5cEl.isJsonArray() || x5cEl.getAsJsonArray().isEmpty()) {
			log("No x5c claim found in key attestation JWT header, skipping certificate chain checks");
			return env;
		}

		List<String> x5c = OIDFJSON.convertJsonArrayToList(x5cEl.getAsJsonArray());
		List<X509Certificate> certs = parseX5cCertificatesFromStrings(x5c);

		String trustAnchorPem = env.getString("vci", "key_attestation_trust_anchor_pem");
		X509Certificate trustAnchorCert = trustAnchorPem != null ? X509CertUtils.parse(trustAnchorPem) : null;

		validateX5cCertificateChain(certs, trustAnchorCert);

		logSuccess("Validated key attestation x5c certificate chain",
			args("x5c", x5c,
				"leaf_cert_subject", certs.get(0).getSubjectX500Principal().getName(),
				"chain_length", certs.size()));

		return env;
	}
}
