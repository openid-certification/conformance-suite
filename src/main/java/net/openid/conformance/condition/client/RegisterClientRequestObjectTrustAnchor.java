package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class RegisterClientRequestObjectTrustAnchor extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		String trustAnchorPem = env.getString("config", "client.request_object_trust_anchor_pem");

		if (trustAnchorPem == null) {
			log("No client request object trust anchor configured, skipping registration");
		} else {
			env.putString("client_request_object_trust_anchor_pem", trustAnchorPem);
			log("Registered client request object trust anchor certificate",
				args("trust_anchor_pem", trustAnchorPem));
		}

		return env;
	}
}
