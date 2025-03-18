package net.openid.conformance.openid.federation.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class UrlDecodeClientIdQueryParameter extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authorization_endpoint_http_request_params")
	public Environment evaluate(Environment env) {

		JsonElement clientIdElement = env.getElementFromObject("authorization_endpoint_http_request_params", "client_id");
		if (clientIdElement == null) {
            throw error("No client_id provided in the request object.", args("claims", clientIdElement));
        }

		String clientId = OIDFJSON.getString(clientIdElement);
		try {
			String decodedClientId = URLDecoder.decode(clientId, StandardCharsets.UTF_8);
			if (!decodedClientId.equals(clientId)) {
				env.putString("authorization_endpoint_http_request_params", "client_id", decodedClientId);
				logSuccess("URL decoded client_id", args("original_client_id", clientId, "decoded_client_id", decodedClientId));
			} else {
				logSuccess("Client_id did not need URL decoding", args("client_id", clientId));
			}
			return env;
		} catch (Exception e) {
			throw error("Failed to URL decode client_id", args("client_id", clientId, "error", e.getMessage()));
		}

	}
}
