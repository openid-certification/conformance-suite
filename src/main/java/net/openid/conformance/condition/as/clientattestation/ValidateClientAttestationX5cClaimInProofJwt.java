package net.openid.conformance.condition.as.clientattestation;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractValidateX5cCertificateChain;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.security.cert.X509Certificate;
import java.util.List;

public class ValidateClientAttestationX5cClaimInProofJwt extends AbstractValidateX5cCertificateChain {

	@Override
	public Environment evaluate(Environment env) {

		JsonElement x5cEl = env.getElementFromObject("client_attestation_object", "header.x5c");
		if (x5cEl == null) {
			throw error("header.x5c is not present");
		}

		if (!x5cEl.isJsonArray()) {
			throw error("header.x5c should be a JSON array", args("x5c", x5cEl));
		}

		List<String> x5c = OIDFJSON.convertJsonArrayToList(x5cEl.getAsJsonArray());

		String trustAnchorPem = env.getString("vci", "client_attestation_trust_anchor_pem");
		if (trustAnchorPem == null) {
			throw error("Client attestation trust anchor is not configured");
		}
		X509Certificate trustAnchorCert = parseTrustAnchorPem(trustAnchorPem);

		List<X509Certificate> certs = parseX5cCertificatesFromStrings(x5c);
		validateX5cCertificateChain(certs, trustAnchorCert);

		logSuccess("Validation of certificate chain in x5c claim successful against trust anchor certificate",
			args("x5c", x5cEl,
				"leaf_cert_subject", certs.get(0).getSubjectX500Principal().getName(),
				"trust_anchor_subject", trustAnchorCert.getSubjectX500Principal().getName(),
				"chain_length", certs.size()));

		return env;
	}
}
