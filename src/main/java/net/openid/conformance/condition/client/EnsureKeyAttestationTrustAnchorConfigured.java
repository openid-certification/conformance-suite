package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.lote.LoteSlot;
import net.openid.conformance.condition.client.lote.LoteTrustAnchors;
import net.openid.conformance.testmodule.Environment;

public class EnsureKeyAttestationTrustAnchorConfigured extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config")
	public Environment evaluate(Environment env) {

		// Read the new key first; fall back to the legacy vci.* key so existing stored
		// test configs keep working through a transition window.
		String trustAnchorPem = env.getString("config", "client_attestation.key_attestation_trust_anchor_pem");
		if (trustAnchorPem == null) {
			trustAnchorPem = env.getString("config", "vci.key_attestation_trust_anchor_pem");
		}
		boolean pemConfigured = trustAnchorPem != null && !trustAnchorPem.isBlank();
		boolean loteConfigured = LoteTrustAnchors.anySlotConfigured(env, LoteSlot.WALLET);
		if (!pemConfigured && !loteConfigured) {
			throw error("Neither the 'Key Attestation Trust Anchor' field in the 'Key Attestation' section nor the '" + LoteSlot.WALLET.fieldLabel
				+ "' field in the 'Client Attestation' section is set in the test configuration. HAIP requires a trust anchor for the key attestation signer, so one of them is required.");
		}

		logSuccess("A trust anchor source for the key attestation signer is configured",
			args("key_attestation_trust_anchor", pemConfigured, "lote", loteConfigured));
		return env;
	}
}
