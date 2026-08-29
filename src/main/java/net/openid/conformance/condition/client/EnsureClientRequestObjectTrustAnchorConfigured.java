package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureClientRequestObjectTrustAnchorConfigured extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config")
	public Environment evaluate(Environment env) {

		String trustAnchorPem = env.getString("config", "client.request_object_trust_anchor_pem");
		// the config form keeps cleared fields as empty strings, so treat blank as absent
		boolean ricalConfigured = Strings.emptyToNull(env.getString("config", "client.rical")) != null
			|| Strings.emptyToNull(env.getString("config", "client.rical_url")) != null;

		if (ricalConfigured) {
			logSuccess("A RICAL is configured as the request object trust source");
			return env;
		}

		if (trustAnchorPem == null || trustAnchorPem.isBlank()) {
			throw error("'Request Object Trust Anchor' field is missing from the 'Client' section in the test configuration (and no 'RICAL' or 'RICAL URL' is configured). A trust source for the request object signing certificate is required for HAIP.");
		}

		logSuccess("Request Object Trust Anchor is configured");
		return env;
	}
}
