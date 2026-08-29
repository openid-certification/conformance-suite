package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class RegisterClientRequestObjectTrustAnchor extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		String trustAnchorPem = env.getString("config", "client.request_object_trust_anchor_pem");
		// the config form keeps cleared fields as empty strings, so treat blank as absent
		boolean ricalConfigured = Strings.emptyToNull(env.getString("config", "client.rical")) != null
			|| Strings.emptyToNull(env.getString("config", "client.rical_url")) != null;

		if (ricalConfigured && trustAnchorPem != null) {
			// Silently dropping either one loses a check the tester asked for: the RICAL may
			// fail to fetch, leaving no trust source at all, while registering both would
			// validate the same chain against two different trust sources.
			throw error("Both a RICAL and the 'Request Object Trust Anchor' are configured in the 'Client' section of the test configuration; the RICAL is a list of reader CAs that replaces the single trust anchor, so please clear whichever of them does not apply");
		} else if (trustAnchorPem == null) {
			log("No client request object trust anchor configured, skipping registration");
		} else {
			env.putString("client_request_object_trust_anchor_pem", trustAnchorPem);
			log("Registered client request object trust anchor certificate",
				args("trust_anchor_pem", trustAnchorPem));
		}

		return env;
	}
}
