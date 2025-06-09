package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class VerifyClientTokenEndpointAuthCredentials extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		JsonObject clientObject = env.getObject("client");
		JsonElement clientSecretEl = clientObject.get("client_secret");

		if (clientSecretEl == null) {
			log("Skipped check for valid credential information for token_endpoint_auth_method in client registration response");
		} else {
			String clientSecret = OIDFJSON.getString(clientSecretEl);
			JsonElement clientSecretExpiresAtEl = clientObject.get("client_secret_expires_at");
			Long clientSecretExpiresAt = clientSecretExpiresAtEl == null ? null : OIDFJSON.getLong(clientSecretExpiresAtEl);
			if (clientSecretExpiresAt == null) {
				throw error("Missing client_secret_expires_at for token_endpoint_auth_method in client registration response");
			}

			logSuccess("Found required credential information for token_endpoint_auth_method in client registration response", args(
					"client_secret", clientSecret, "client_secret_expires_at", clientSecretExpiresAt));
		}

		return env;
	}
}
