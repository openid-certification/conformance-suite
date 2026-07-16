package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.net.URI;

public class ValidateOpenBankingBrazilCibaDynamicRegistrationResponse extends AbstractCondition {

	private static final String CIBA_GRANT_TYPE = "urn:openid:params:grant-type:ciba";

	@Override
	@PreEnvironment(required = { "dynamic_registration_request", "client" })
	public Environment evaluate(Environment env) {
		JsonObject request = env.getObject("dynamic_registration_request");
		JsonObject client = env.getObject("client");

		ensureCibaGrantType(client);
		ensureMatchesRequest(request, client, "redirect_uris");
		ensureMatchesRequest(request, client, "jwks_uri");
		ensureMatchesRequest(request, client, "backchannel_token_delivery_mode");
		ensureMatchesRequest(request, client, "backchannel_client_notification_endpoint");
		ensureMatchesRequest(request, client, "backchannel_authentication_request_signing_alg");
		ensureMatchesRequest(request, client, "token_endpoint_auth_method");
		ensureMatchesRequestIfRequested(request, client, "token_endpoint_auth_signing_alg");
		ensureMatchesRequest(request, client, "id_token_signed_response_alg");
		ensureMatchesRequest(request, client, "tls_client_certificate_bound_access_tokens");
		ensurePingMode(client);
		ensureHttpsNotificationEndpoint(client);
		ensurePs256RequestSigning(client);
		ensureUserCodeIsAbsentOrFalse(client);
		ensureNoInlineJwks(client);

		logSuccess("Validated Open Finance Brazil CIBA dynamic registration response metadata",
			args("client", client));
		return env;
	}

	private void ensureCibaGrantType(JsonObject client) {
		JsonElement grantTypesElement = client.get("grant_types");
		if (grantTypesElement == null || !grantTypesElement.isJsonArray()) {
			throw error("Dynamic registration response must contain a grant_types array",
				args("grant_types", grantTypesElement));
		}

		JsonArray grantTypes = grantTypesElement.getAsJsonArray();
		for (JsonElement grantType : grantTypes) {
			if (grantType.isJsonPrimitive()
				&& grantType.getAsJsonPrimitive().isString()
				&& CIBA_GRANT_TYPE.equals(OIDFJSON.getString(grantType))) {
				return;
			}
		}
		throw error("Dynamic registration response does not contain the CIBA grant type",
			args("grant_types", grantTypes, "required", CIBA_GRANT_TYPE));
	}

	private void ensureMatchesRequest(JsonObject request, JsonObject client, String fieldName) {
		JsonElement requested = request.get(fieldName);
		JsonElement registered = client.get(fieldName);
		if (requested == null || registered == null || !requested.equals(registered)) {
			throw error("Dynamic registration response metadata does not match the request",
				args("field", fieldName, "requested", requested, "registered", registered));
		}
	}

	private void ensureMatchesRequestIfRequested(JsonObject request, JsonObject client, String fieldName) {
		if (request.has(fieldName)) {
			ensureMatchesRequest(request, client, fieldName);
		}
	}

	private void ensurePingMode(JsonObject client) {
		if (!"ping".equals(getRequiredString(client, "backchannel_token_delivery_mode"))) {
			throw error("Dynamic registration response must retain ping mode",
				args("backchannel_token_delivery_mode", client.get("backchannel_token_delivery_mode")));
		}
	}

	private void ensureHttpsNotificationEndpoint(JsonObject client) {
		String endpoint = getRequiredString(client, "backchannel_client_notification_endpoint");
		URI endpointUri;
		try {
			endpointUri = URI.create(endpoint);
		} catch (IllegalArgumentException invalidUri) {
			throw error("Dynamic registration response contains an invalid notification endpoint URI",
				invalidUri, args("backchannel_client_notification_endpoint", endpoint));
		}
		if (!endpointUri.isAbsolute() || !"https".equalsIgnoreCase(endpointUri.getScheme())) {
			throw error("Dynamic registration response notification endpoint must use HTTPS",
				args("backchannel_client_notification_endpoint", endpoint));
		}
	}

	private void ensurePs256RequestSigning(JsonObject client) {
		String signingAlgorithm = getRequiredString(client,
			"backchannel_authentication_request_signing_alg");
		if (!"PS256".equals(signingAlgorithm)) {
			throw error("Dynamic registration response must retain PS256 CIBA request signing",
				args("backchannel_authentication_request_signing_alg", signingAlgorithm));
		}
	}

	private void ensureUserCodeIsAbsentOrFalse(JsonObject client) {
		JsonElement userCode = client.get("backchannel_user_code_parameter");
		if (userCode == null) {
			return;
		}
		if (!userCode.isJsonPrimitive()
			|| !userCode.getAsJsonPrimitive().isBoolean()
			|| OIDFJSON.getBoolean(userCode)) {
			throw error("Dynamic registration response must not enable the CIBA user code parameter",
				args("backchannel_user_code_parameter", userCode));
		}
	}

	private void ensureNoInlineJwks(JsonObject client) {
		if (client.has("jwks")) {
			throw error("Dynamic registration response must retain jwks_uri instead of inline jwks",
				args("jwks", client.get("jwks"), "jwks_uri", client.get("jwks_uri")));
		}
	}

	private String getRequiredString(JsonObject client, String fieldName) {
		JsonElement value = client.get(fieldName);
		if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
			throw error("Dynamic registration response metadata must be a string",
				args("field", fieldName, "value", value));
		}
		return OIDFJSON.getString(value);
	}
}
