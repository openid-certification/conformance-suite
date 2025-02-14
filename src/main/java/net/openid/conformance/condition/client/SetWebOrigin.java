package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.net.URI;
import java.net.URISyntaxException;

public class SetWebOrigin extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "base_url")
	@PostEnvironment(strings = "origin")
	public Environment evaluate(Environment env) {
		String baseUrlStr = env.getString("base_url");

		if (baseUrlStr.isEmpty()) {
			throw error("Base URL is empty");
		}
		String origin;
		try {
			URI baseUrl = new URI(baseUrlStr);

			// strip off path/query/fragment
			URI originUri = new URI(
				baseUrl.getScheme(),
				baseUrl.getAuthority(),
				null,
				null,
				null
			);

			origin = originUri.toString();
		} catch (URISyntaxException e) {
			throw error("Couldn't parse baseurl as URL", e, args("baseurl", baseUrlStr));
		}

		env.putString("origin", origin);

		log("Set origin from conformance suite base url",
			args("base_url", baseUrlStr,
				"origin", origin));

		return env;
	}

}
