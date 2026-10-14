package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.client.lote.AbstractRegisterTrustSource;
import net.openid.conformance.condition.client.lote.LoteSlot;
import net.openid.conformance.condition.client.lote.LoteTrustAnchors;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

public class RegisterClientRequestObjectTrustAnchor extends AbstractRegisterTrustSource {

	@Override
	public Environment evaluate(Environment env) {

		String trustAnchorPem = env.getString("config", "client.request_object_trust_anchor_pem");
		// the config form keeps cleared fields as empty strings, so treat blank as absent
		boolean ricalConfigured = Strings.emptyToNull(env.getString("config", "client.rical")) != null
			|| Strings.emptyToNull(env.getString("config", "client.rical_url")) != null;

		List<String> sources = LoteTrustAnchors.configuredSources(env, trustAnchorPem,
			"'Request Object Trust Anchor' field in the 'Client' section of the test configuration", LoteSlot.WRPAC);
		if (ricalConfigured) {
			sources.add(0, "'RICAL' or 'RICAL URL' field in the 'Client' section of the test configuration");
		}

		requireAtMostOneTrustSource(sources, "request object signing certificate");
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
