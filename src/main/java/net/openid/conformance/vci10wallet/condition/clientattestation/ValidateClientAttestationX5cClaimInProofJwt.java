package net.openid.conformance.vci10wallet.condition.clientattestation;

import com.google.gson.JsonElement;
import com.nimbusds.jose.util.X509CertUtils;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

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

		// There is currently no requirement for the x5c cert, so it may be self-signed
		try {
			leafCert.verify(leafCert.getPublicKey());
//			throw error("Certificate used in x5c claim must not be self-signed!",
//				args("x5c", x5cEl, "leaf_cert_pem", leafCertPem));
			log("Certificate used in x5c claim appears to be self-signed.");
		} catch (Exception ignored) {
		}

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
