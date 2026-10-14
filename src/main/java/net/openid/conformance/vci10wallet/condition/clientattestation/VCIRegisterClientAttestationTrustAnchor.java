package net.openid.conformance.vci10wallet.condition.clientattestation;

import net.openid.conformance.condition.client.lote.AbstractRegisterTrustSource;
import net.openid.conformance.condition.client.lote.LoteSlot;
import net.openid.conformance.condition.client.lote.LoteTrustAnchors;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

public class VCIRegisterClientAttestationTrustAnchor extends AbstractRegisterTrustSource {

	@Override
	public Environment evaluate(Environment env) {

		// Read the new key first; fall back to the legacy vci.* key so existing stored
		// test configs keep working through a transition window.
		String clientAttestationTrustAnchorPem = env.getString("config", "client_attestation.trust_anchor");
		if (clientAttestationTrustAnchorPem == null) {
			clientAttestationTrustAnchorPem = env.getString("config", "vci.client_attestation_trust_anchor");
		}
		List<String> sources = LoteTrustAnchors.configuredSources(env, clientAttestationTrustAnchorPem,
			"'Client Attestation Trust Anchor' field in the 'Client Attestation' section of the test configuration", LoteSlot.WALLET);
		requireAtMostOneTrustSource(sources, "client attestation signing certificate");
		if (clientAttestationTrustAnchorPem == null) {
			// the wallet providers list of trusted entities then supplies the anchors
			log("No 'Client Attestation Trust Anchor' configured; the client attestation chain is validated against the wallet providers list of trusted entities only");
			return env;
		}
		env.putString("vci", "client_attestation_trust_anchor_pem", clientAttestationTrustAnchorPem);

		log("Register trust anchor certificate for client attestation", args("trust_anchor_pem", clientAttestationTrustAnchorPem));

		return env;
	}
}
