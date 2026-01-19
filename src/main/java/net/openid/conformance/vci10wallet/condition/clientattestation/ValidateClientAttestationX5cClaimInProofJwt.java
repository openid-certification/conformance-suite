package net.openid.conformance.vci10wallet.condition.clientattestation;

import com.google.gson.JsonElement;
import com.nimbusds.jose.util.X509CertUtils;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.X509CertificateUtil;

import java.security.cert.X509Certificate;
import java.util.List;

public class ValidateClientAttestationX5cClaimInProofJwt extends AbstractCondition {

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

		String encodedLeafCert = x5c.get(0);

		byte[] leafCertDer = java.util.Base64.getDecoder().decode(encodedLeafCert);
		X509Certificate leafCert = X509CertUtils.parse(leafCertDer);
		String leafCertPem = X509CertUtils.toPEMString(leafCert);

		String trustAnchorPem = env.getString("vci", "client_attestation_trust_anchor_pem");
		X509Certificate trustAnchorCert = X509CertUtils.parse(trustAnchorPem);

		// time validity
		try {
			leafCert.checkValidity();
		} catch (Exception e) {
			throw error("Certificate used in x5c claim must be valid!",
				args("x5c", x5cEl, "leaf_cert_pem", leafCertPem, "error", e.getMessage()));
		}

		// Per HAIP section 4.5.1: Client attestation certificate must NOT be self-signed
		if (X509CertificateUtil.isSelfSigned(leafCert)) {
			throw error("Certificate used in x5c claim must not be self-signed (HAIP section 4.5.1)",
				args("x5c", x5cEl, "leaf_cert_pem", leafCertPem));
		}

		log("Client attestation certificate is not self-signed (as required)",
			args("leaf_cert_subject", leafCert.getSubjectX500Principal().getName()));

		// Per HAIP section 4.5.1: Trust anchor MUST NOT be included in the x5c chain
		for (String encodedCert : x5c) {
			byte[] certDer = java.util.Base64.getDecoder().decode(encodedCert);
			X509Certificate cert = X509CertUtils.parse(certDer);
			if (cert.equals(trustAnchorCert)) {
				throw error("Trust anchor certificate MUST NOT be included in x5c chain (HAIP section 4.5.1)",
					args("x5c", x5cEl, "trust_anchor_pem", trustAnchorPem));
			}
		}
		log("Trust anchor is not included in x5c chain (as required)");

		// chain signature check (leaf signed by trust anchor)
		try {
			leafCert.verify(trustAnchorCert.getPublicKey());
		} catch (Exception e) {
			throw error("Certificate used in x5c claim must be verifiable by trust anchor certificate",
				args("x5c", x5cEl, "leaf_cert_pem", leafCertPem, "trust_anchor_pem", trustAnchorPem, "error", e.getMessage()));
		}

		logSuccess("Validation of certificate used in x5c claim successful against trust anchor certificate",
			args("x5c", x5cEl, "leaf_cert_pem", leafCertPem, "trust_anchor_pem", trustAnchorPem)
		);

		return env;
	}
}
