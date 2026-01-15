package net.openid.conformance.vci10wallet.condition.clientattestation;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class VCIRegisterClientAttestationTrustAnchor extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		String clientAttestationTrustAnchorPem = env.getString("config", "vci.client_attestation_trust_anchor");
		env.putString("vci", "client_attestation_trust_anchor_pem", clientAttestationTrustAnchorPem);

		log("Register trust anchor certificate for client attestation", args("trust_anchor_pem", clientAttestationTrustAnchorPem));

		return env;
	}
}
