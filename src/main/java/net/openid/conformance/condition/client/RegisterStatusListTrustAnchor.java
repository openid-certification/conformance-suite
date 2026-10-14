package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.client.lote.LoteSlot;
import net.openid.conformance.condition.client.lote.LoteTrustAnchors;
import net.openid.conformance.testmodule.Environment;

public class RegisterStatusListTrustAnchor extends AbstractCondition {

	/** Set when a status list trust anchor PEM or a PID or mDL providers LoTE URL is configured. */
	public static final String ENV_TRUST_SOURCE_CONFIGURED = "status_list_trust_source_configured";

	@Override
	public Environment evaluate(Environment env) {

		String trustAnchorPem = env.getString("config", "credential.status_list_trust_anchor_pem");

		if (trustAnchorPem == null) {
			log("No status list trust anchor configured, skipping registration");
		} else {
			env.putString("status_list_trust_anchor_pem", trustAnchorPem);
			log("Registered status list trust anchor certificate",
				args("trust_anchor_pem", trustAnchorPem));
		}
		// the chain checks that are optional outside HAIP run when any trust source is configured:
		// the PEM, or a list of trusted entities whose revocation services SetupLoteFromConfiguration extracts
		if (trustAnchorPem != null || LoteTrustAnchors.anySlotConfigured(env, LoteSlot.PID, LoteSlot.MDL)) {
			env.putString(ENV_TRUST_SOURCE_CONFIGURED, "true");
		}

		return env;
	}
}
