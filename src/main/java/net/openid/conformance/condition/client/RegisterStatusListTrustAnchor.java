package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class RegisterStatusListTrustAnchor extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		String trustAnchorPem = env.getString("config", "credential.status_list_trust_anchor_pem");

		if (trustAnchorPem == null) {
			log("No status list trust anchor configured, skipping registration");
		} else {
			env.putString("status_list_trust_anchor_pem", trustAnchorPem);
			log("Registered status list trust anchor certificate",
				args("trust_anchor_pem", trustAnchorPem));
		}

		return env;
	}
}
