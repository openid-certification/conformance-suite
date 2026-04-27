package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureStatusListTrustAnchorConfigured extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config")
	public Environment evaluate(Environment env) {

		String trustAnchorPem = env.getString("config", "credential.status_list_trust_anchor_pem");
		if (trustAnchorPem == null || trustAnchorPem.isBlank()) {
			throw error("'Status List Trust Anchor' field is missing from the 'Credential' section in the test configuration. It is required for HAIP.");
		}

		logSuccess("Status List Trust Anchor is configured");
		return env;
	}
}
