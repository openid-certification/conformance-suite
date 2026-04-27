package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureKeyAttestationTrustAnchorConfigured extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config")
	public Environment evaluate(Environment env) {

		String trustAnchorPem = env.getString("config", "vci.key_attestation_trust_anchor_pem");
		if (trustAnchorPem == null || trustAnchorPem.isBlank()) {
			throw error("'Key Attestation Trust Anchor' field is missing from the 'Key Attestation' section in the test configuration. It is required for HAIP.");
		}

		logSuccess("Key Attestation Trust Anchor is configured");
		return env;
	}
}
