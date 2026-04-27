package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureClientRequestObjectTrustAnchorConfigured extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config")
	public Environment evaluate(Environment env) {

		String trustAnchorPem = env.getString("config", "client.request_object_trust_anchor_pem");
		if (trustAnchorPem == null || trustAnchorPem.isBlank()) {
			throw error("'Request Object Trust Anchor' field is missing from the 'Client' section in the test configuration. It is required for HAIP.");
		}

		logSuccess("Request Object Trust Anchor is configured");
		return env;
	}
}
