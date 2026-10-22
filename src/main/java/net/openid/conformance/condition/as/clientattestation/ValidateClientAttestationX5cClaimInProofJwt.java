package net.openid.conformance.condition.as.clientattestation;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractValidateX5cCertificateChain;
import net.openid.conformance.condition.client.lote.LoteTrustAnchors;
import net.openid.conformance.condition.client.lote.TrustPurpose;
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

		// the configured 'Client Attestation Trust Anchor', or the wallet providers listed in the configured LoTE
		LoteTrustAnchors.TrustAnchorSet anchors = resolveTrustAnchors(env, TrustPurpose.CLIENT_ATTESTATION);
		if (anchors.isEmpty()) {
			throw error("No trust anchor for the client attestation: none of " + TrustPurpose.CLIENT_ATTESTATION.fieldsDescription()
				+ " of the test configuration yielded one");
		}

		List<X509Certificate> certs = parseX5cCertificatesFromStrings(x5c);
		X509Certificate matched = validateX5cCertificateChainAgainst(certs, anchors);

		logSuccess("Validation of certificate chain in x5c claim successful against a trust anchor certificate",
			args("x5c", x5cEl,
				"leaf_cert_subject", certs.get(0).getSubjectX500Principal().getName(),
				"trust_anchor_subject", matched.getSubjectX500Principal().getName(),
				"matched_trust_anchor", anchors.sourceOf(matched),
				"chain_length", certs.size()));

		return env;
	}
}
