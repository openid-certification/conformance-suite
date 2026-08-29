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
			// a configured RICAL supersedes the single trust anchor, mirroring how a
			// configured VICAL supersedes the credential trust anchor for mdoc chains
			log("A RICAL is configured, which supersedes the 'Request Object Trust Anchor'; the request object chain will be validated against the RICAL instead");
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
