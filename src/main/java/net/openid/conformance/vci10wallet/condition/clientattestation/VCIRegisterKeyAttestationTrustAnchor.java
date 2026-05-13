package net.openid.conformance.vci10wallet.condition.clientattestation;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class VCIRegisterKeyAttestationTrustAnchor extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		// Read the new key first; fall back to the legacy vci.* key so existing stored
		// test configs keep working through a transition window.
		String keyAttestationTrustAnchorPem = env.getString("config", "client_attestation.key_attestation_trust_anchor_pem");
		if (keyAttestationTrustAnchorPem == null) {
			keyAttestationTrustAnchorPem = env.getString("config", "vci.key_attestation_trust_anchor_pem");
		}

		if (keyAttestationTrustAnchorPem == null) {
			log("Skipping registration of empty trust anchor certificate for key attestation");
		} else {
			env.putString("vci", "key_attestation_trust_anchor_pem", keyAttestationTrustAnchorPem);
			log("Register trust anchor certificate for key attestation", args("trust_anchor_pem", keyAttestationTrustAnchorPem));
		}

		return env;
	}
}
