package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Set;

public class VerifyClientTokenEndpointAuthCredentials extends AbstractCondition {

	public static final Set<String> TOKEN_ENDPOINT_AUTH_METHODS_WITH_SECRET = Set.of("client_secret_basic", "client_secret_post", "client_secret_jwt");

	@Override
	public Environment evaluate(Environment env) {

		String authMethod = env.getString("dynamic_registration_request", "token_endpoint_auth_method");
		if (TOKEN_ENDPOINT_AUTH_METHODS_WITH_SECRET.contains(authMethod)) {
			JsonObject clientObject = env.getObject("client");
			JsonElement clientSecretEl = clientObject.get("client_secret");
			String clientSecret = clientSecretEl == null ? null : OIDFJSON.getString(clientSecretEl);
			if (Strings.isNullOrEmpty(clientSecret)) {
				throw error("Missing client_secret for token_endpoint_auth_method in client registration response", args("token_endpoint_auth_method", authMethod));
			}

			JsonElement clientSecretExpiresAtEl = clientObject.get("client_secret_expires_at");
			Long clientSecretExpiresAt = clientSecretExpiresAtEl == null ? null : OIDFJSON.getLong(clientSecretExpiresAtEl);
			if (clientSecretExpiresAt == null) {
				throw error("Missing client_secret_expires_at for token_endpoint_auth_method in client registration response", args("token_endpoint_auth_method", authMethod));
			}

			logSuccess("Found required credential information for token_endpoint_auth_method in client registration response", args("token_endpoint_auth_method", authMethod,
				"client_secret", clientSecret, "client_secret_expires_at", clientSecretExpiresAt));
		} else {
			log("Skipped check for valid credential information for token_endpoint_auth_method in client registration response", args("token_endpoint_auth_method", authMethod));
		}

		return env;
	}
}
