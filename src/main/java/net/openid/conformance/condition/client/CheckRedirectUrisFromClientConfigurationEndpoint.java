package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class CheckRedirectUrisFromClientConfigurationEndpoint extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "registration_client_endpoint_response" }, strings = "redirect_uri")
	public Environment evaluate(Environment env) {
		String expectedRedirect = env.getString("redirect_uri");

		JsonArray redirectUris = getJsonArrayFromEnvironment(env, "registration_client_endpoint_response", "body_json.redirect_uris", "redirect_uris in client config response");

		if (redirectUris.size() != 1) {
			throw error("One redirect uri was requested but server returned "+redirectUris.size(),
				args("expected", expectedRedirect, "actual", redirectUris));
		}

		JsonElement el = redirectUris.get(0);
		if (!el.isJsonPrimitive() || !el.getAsJsonPrimitive().isString()) {
			throw error("Entry in redirect uri array was not a string",
				args("expected", expectedRedirect, "actual", redirectUris));
		}
		String actual = OIDFJSON.getString(el);
		if (!actual.equals(expectedRedirect)) {
			throw error("redirect uri in client configuration response does not match expected redirect uri.",
				args("expected", expectedRedirect, "actual", redirectUris));
		}

		logSuccess("Client configuration endpoint returned correct redirect_uris.");

		return env;
	}
}
