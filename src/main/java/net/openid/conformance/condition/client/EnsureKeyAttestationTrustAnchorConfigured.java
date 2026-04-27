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
			throw error("'Key Attestation Trust Anchor' must be configured in the 'Key Attestation' section of the test configuration for HAIP tests");
		}

		logSuccess("Key attestation trust anchor PEM is configured");
		return env;
	}
}
