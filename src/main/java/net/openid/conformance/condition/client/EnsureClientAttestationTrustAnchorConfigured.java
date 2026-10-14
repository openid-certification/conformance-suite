package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.lote.LoteSlot;
import net.openid.conformance.condition.client.lote.LoteTrustAnchors;
import net.openid.conformance.testmodule.Environment;

/**
 * With client attestation as the client authentication method the suite, as the issuer, must be
 * able to chain the wallet's attestation signer to something: the configured 'Client Attestation
 * Trust Anchor' or the wallet providers listed in the configured LoTE.
 */
public class EnsureClientAttestationTrustAnchorConfigured extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config")
	public Environment evaluate(Environment env) {
		// the legacy vci.* key is read as a fallback so existing stored configurations keep working
		String trustAnchorPem = env.getString("config", "client_attestation.trust_anchor");
		if (trustAnchorPem == null) {
			trustAnchorPem = env.getString("config", "vci.client_attestation_trust_anchor");
		}
		boolean pemConfigured = trustAnchorPem != null && !trustAnchorPem.isBlank();
		boolean loteConfigured = LoteTrustAnchors.anySlotConfigured(env, LoteSlot.WALLET);
		if (!pemConfigured && !loteConfigured) {
			throw error("Neither the 'Client Attestation Trust Anchor' nor the '" + LoteSlot.WALLET.fieldLabel
				+ "' field is set in the 'Client Attestation' section of the test configuration. One of them is required when"
				+ " client attestation is the client authentication method.");
		}
		logSuccess("A trust anchor source for the client attestation signer is configured",
			args("client_attestation_trust_anchor", pemConfigured, "lote", loteConfigured));
		return env;
	}
}
