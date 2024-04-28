package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.net.URI;
import java.net.URISyntaxException;

public class SetClientIdToResponseUriHostnameIfUnset extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config")
	public Environment evaluate(Environment env) {

		String client_id = env.getString("config", "client.client_id");
		if (!Strings.isNullOrEmpty(client_id)) {
			log("client_id is already set, not setting it to our hostname", args("client_id", client_id));
			return env;
		}

		String responseUri = env.getString("response_uri");
		if (Strings.isNullOrEmpty(responseUri)) {
			throw error("response_uri is not in use, please set a client_id in test configuration.");
		}
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
