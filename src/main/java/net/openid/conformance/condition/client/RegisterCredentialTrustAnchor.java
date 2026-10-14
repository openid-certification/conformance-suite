package net.openid.conformance.condition.client;

import net.openid.conformance.condition.client.lote.AbstractRegisterTrustSource;
import net.openid.conformance.condition.client.lote.LoteSlot;
import net.openid.conformance.condition.client.lote.LoteTrustAnchors;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

public class RegisterCredentialTrustAnchor extends AbstractRegisterTrustSource {

	/** Set when a credential trust anchor PEM or a PID or mDL providers LoTE URL is configured. */
	public static final String ENV_TRUST_SOURCE_CONFIGURED = "credential_trust_source_configured";

	@Override
	public Environment evaluate(Environment env) {

		String trustAnchorPem = env.getString("config", "credential.trust_anchor_pem");
		List<String> sources = LoteTrustAnchors.configuredSources(env, trustAnchorPem,
			"'Credential Trust Anchor' field in the 'Credential Issuer' section of the test configuration", LoteSlot.PID, LoteSlot.MDL);
		requireAtMostOneTrustSource(sources, "credential signing certificate");

		if (trustAnchorPem == null) {
			log("No credential trust anchor configured, skipping registration");
		} else {
			env.putString("credential_trust_anchor_pem", trustAnchorPem);
			log("Registered credential trust anchor certificate",
				args("trust_anchor_pem", trustAnchorPem));
		}
		// the chain checks that are optional outside HAIP run when any trust source is configured:
		// the PEM, or a list of trusted entities whose anchors SetupLoteFromConfiguration extracts
		if (!sources.isEmpty()) {
			env.putString(ENV_TRUST_SOURCE_CONFIGURED, "true");
		}

		return env;
	}
}
