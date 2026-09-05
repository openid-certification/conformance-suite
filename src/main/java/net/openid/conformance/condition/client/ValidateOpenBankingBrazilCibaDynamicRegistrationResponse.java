package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

public class ValidateOpenBankingBrazilCibaDynamicRegistrationResponse extends AbstractCondition {

	private static final String CIBA_GRANT_TYPE = "urn:openid:params:grant-type:ciba";
	private static final Set<String> ALLOWED_TOKEN_ENDPOINT_AUTH_METHODS = Set.of(
		"tls_client_auth",
		"self_signed_tls_client_auth",
		"private_key_jwt");

	@Override
	@PreEnvironment(required = { "software_statement_assertion", "client" })
	public Environment evaluate(Environment env) {
		JsonObject client = env.getObject("client");

		ensureCibaGrantType(client);
		ensureRedirectUrisMatchSoftwareStatement(env, client);
		ensureJwksUriMatchesSoftwareStatement(env, client);
		ensurePingMode(client);
		ensureHttpsNotificationEndpoint(client);
		ensurePs256RequestSigning(client);
		ensureTokenEndpointAuthenticationIsAllowed(client);
		ensureProfileSigningAndEncryption(client);
		ensureCertificateBoundAccessTokens(client);
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

	private void ensureRedirectUrisMatchSoftwareStatement(Environment env, JsonObject client) {
		JsonElement softwareRedirectUris = env.getElementFromObject(
			"software_statement_assertion", "claims.software_redirect_uris");
		if (softwareRedirectUris == null || softwareRedirectUris.isJsonNull()) {
			throw error("Software statement does not contain software_redirect_uris");
		}

		JsonElement registeredRedirectUris = getRequiredResponseMetadata(client, "redirect_uris");
		Set<String> softwareStatementUris = getRedirectUriSet(softwareRedirectUris, "software statement");
		Set<String> registeredUris = getRedirectUriSet(registeredRedirectUris, "response");
		if (registeredUris.isEmpty()) {
			throw error("Dynamic registration response redirect_uris must not be empty");
		}
		if (!softwareStatementUris.containsAll(registeredUris)) {
			throw error("Dynamic registration response redirect_uris are not contained in software_redirect_uris",
				args("software_redirect_uris", softwareRedirectUris,
					"registered_redirect_uris", registeredRedirectUris));
		}
	}

	private void ensureJwksUriMatchesSoftwareStatement(Environment env, JsonObject client) {
		String softwareJwksUri = env.getString(
			"software_statement_assertion", "claims.software_jwks_uri");
		String registeredJwksUri = getRequiredString(client, "jwks_uri");
		if (softwareJwksUri == null || !softwareJwksUri.equals(registeredJwksUri)) {
			throw error("Dynamic registration response jwks_uri does not match software_jwks_uri",
				args("software_jwks_uri", softwareJwksUri, "registered_jwks_uri", registeredJwksUri));
		}
	}

	private JsonElement getRequiredResponseMetadata(JsonObject client, String fieldName) {
		JsonElement registered = client.get(fieldName);
		if (registered == null || registered.isJsonNull()) {
			throw error("Dynamic registration response does not contain required metadata: " + fieldName,
				args("field", fieldName));
		}
		return registered;
	}

	private Set<String> getRedirectUriSet(JsonElement redirectUrisElement, String source) {
		if (!redirectUrisElement.isJsonArray()) {
			throw error("Dynamic registration " + source + " redirect_uris metadata must be an array",
				args("redirect_uris", redirectUrisElement));
		}

		Set<String> redirectUris = new HashSet<>();
		for (JsonElement redirectUri : redirectUrisElement.getAsJsonArray()) {
			if (!redirectUri.isJsonPrimitive() || !redirectUri.getAsJsonPrimitive().isString()) {
				throw error("Dynamic registration " + source + " redirect_uris metadata must contain only strings",
					args("redirect_uris", redirectUrisElement));
			}
			redirectUris.add(OIDFJSON.getString(redirectUri));
		}
		return redirectUris;
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
		if (!endpointUri.isAbsolute()
			|| !"https".equalsIgnoreCase(endpointUri.getScheme())
			|| endpointUri.getHost() == null) {
			throw error("Dynamic registration response notification endpoint must be an HTTPS URL",
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

	private void ensureTokenEndpointAuthenticationIsAllowed(JsonObject client) {
		String authMethod = getRequiredString(client, "token_endpoint_auth_method");
		if (!ALLOWED_TOKEN_ENDPOINT_AUTH_METHODS.contains(authMethod)) {
			throw error("Dynamic registration response token_endpoint_auth_method is not allowed",
				args("token_endpoint_auth_method", authMethod,
					"allowed", ALLOWED_TOKEN_ENDPOINT_AUTH_METHODS));
		}

		// OpenID Connect Registration section 2 defines this metadata only for JWT authentication.
		// An unused server default must not make an mTLS registration fail.
		if ("private_key_jwt".equals(authMethod)) {
			ensurePs256(client, "token_endpoint_auth_signing_alg");
		}
	}

	private void ensureProfileSigningAndEncryption(JsonObject client) {
		ensurePs256(client, "id_token_signed_response_alg");
		ensureStringValue(client, "id_token_encrypted_response_alg", "RSA-OAEP");
		ensureStringValue(client, "id_token_encrypted_response_enc", "A256GCM");
	}

	private void ensurePs256(JsonObject client, String fieldName) {
		ensureStringValue(client, fieldName, "PS256");
	}

	private void ensureStringValue(JsonObject client, String fieldName, String requiredValue) {
		String registeredValue = getRequiredString(client, fieldName);
		if (!requiredValue.equals(registeredValue)) {
			throw error("Dynamic registration response metadata does not meet Open Finance Brazil requirements",
				args("field", fieldName, "registered", registeredValue, "required", requiredValue));
		}
	}

	private void ensureCertificateBoundAccessTokens(JsonObject client) {
		JsonElement value = getRequiredResponseMetadata(client,
			"tls_client_certificate_bound_access_tokens");
		if (!value.isJsonPrimitive()
			|| !value.getAsJsonPrimitive().isBoolean()
			|| !OIDFJSON.getBoolean(value)) {
			throw error("Dynamic registration response must enable certificate-bound access tokens",
				args("tls_client_certificate_bound_access_tokens", value));
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
		JsonElement value = getRequiredResponseMetadata(client, fieldName);
		if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
			throw error("Dynamic registration response metadata must be a string",
				args("field", fieldName, "value", value));
		}
		return OIDFJSON.getString(value);
	}
}
