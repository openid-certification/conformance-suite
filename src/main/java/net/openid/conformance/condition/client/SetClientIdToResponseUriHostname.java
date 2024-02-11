package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.net.URI;
import java.net.URISyntaxException;

public class SetClientIdToResponseUriHostname extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "response_uri", required = "config")
	public Environment evaluate(Environment env) {
		String responseUri = env.getString("response_uri");
		String hostname;

		try {
			URI parsedResponseUri = new URI(responseUri);

			hostname = parsedResponseUri.getHost();
		} catch (URISyntaxException e) {
			throw error("Couldn't parse response_uri as URL", e, args("response_uri", responseUri));
		}

		env.putString("config", "client.client_id", hostname);

		log("Set client_id to response URI hostname",
			args("client_id", responseUri));

		return env;
	}

}
