package net.openid.conformance.condition.as;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

public class ExtractClientCredentialsFromBasicAuthorizationHeader extends AbstractCondition {

	@Override
	@PreEnvironment(required = "token_endpoint_request")
	@PostEnvironment(required = "client_authentication")
	public Environment evaluate(Environment env) {

		if (env.containsObject("client_authentication")) {
			throw error("Found existing client authentication");
		}

		String auth = env.getString("token_endpoint_request", "headers.authorization");

		if (Strings.isNullOrEmpty(auth)) {
			throw error("This test expected the client to perform client_secret_basic client authorization, but the incoming http request does not contain an authorization header");
		}

		if (!auth.toLowerCase().startsWith("basic")) {
			throw error("Not a basic authorization header", args("auth", auth));
		}

		// parse the HTTP Basic Auth

		String decoded = new String(Base64.getDecoder().decode( // base64 decode
			auth.substring("Basic ".length()))); // strip off the "Basic " prefix first though

		List<String> parts = Lists.newArrayList(Splitter.on(":").split(decoded)); // split the results at a colon to get username:password (in our case, clientId:clientSecret)

		if (parts.size() != 2) {
			// we don't have two parts
			throw error("Unexpected number of parts to authorization header", args("basic_auth", parts));
		}

		String clientId = URLDecoder.decode(parts.get(0), StandardCharsets.UTF_8);
		String clientSecret = URLDecoder.decode(parts.get(1), StandardCharsets.UTF_8);

		JsonObject clientAuthentication = new JsonObject();
		clientAuthentication.addProperty("client_id", clientId);
		clientAuthentication.addProperty("client_secret", clientSecret);
		clientAuthentication.addProperty("method", "client_secret_basic");

		env.putObject("client_authentication", clientAuthentication);

		logSuccess("Extracted client authentication", clientAuthentication);

		return env;

	}

}
