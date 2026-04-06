package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class RegisterCredentialTrustAnchor extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		String trustAnchorPem = env.getString("config", "credential.trust_anchor_pem");

		if (trustAnchorPem == null) {
			log("No credential trust anchor configured, skipping registration");
		} else {
			env.putString("credential_trust_anchor_pem", trustAnchorPem);
			log("Registered credential trust anchor certificate",
				args("trust_anchor_pem", trustAnchorPem));
		}

		return env;
	}
}
