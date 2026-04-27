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
			throw error("'Request Object Trust Anchor PEM' must be configured in the 'Client' section of the test configuration for HAIP tests");
		}

		logSuccess("Client request object trust anchor PEM is configured");
		return env;
	}
}
