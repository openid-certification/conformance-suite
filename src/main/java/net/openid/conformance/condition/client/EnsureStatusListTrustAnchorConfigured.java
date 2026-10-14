package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.lote.LoteSlot;
import net.openid.conformance.condition.client.lote.LoteTrustAnchors;
import net.openid.conformance.testmodule.Environment;

public class EnsureStatusListTrustAnchorConfigured extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config")
	public Environment evaluate(Environment env) {

		String trustAnchorPem = env.getString("config", "credential.status_list_trust_anchor_pem");
		boolean pemConfigured = trustAnchorPem != null && !trustAnchorPem.isBlank();
		// a LoTE's revocation services carry the status list signers' anchors
		boolean loteConfigured = LoteTrustAnchors.anySlotConfigured(env, LoteSlot.PID, LoteSlot.MDL);
		if (!pemConfigured && !loteConfigured) {
			throw error("None of the 'Status List Trust Anchor', '" + LoteSlot.PID.fieldLabel + "' and '" + LoteSlot.MDL.fieldLabel
				+ "' fields is set in the 'Credential Issuer' section in the test configuration. HAIP requires a trust anchor for the status list signer, so one of them is required.");
		}

		logSuccess("A trust anchor source for the status list signer is configured",
			args("status_list_trust_anchor", pemConfigured, "lote", loteConfigured));
		return env;
	}
}
