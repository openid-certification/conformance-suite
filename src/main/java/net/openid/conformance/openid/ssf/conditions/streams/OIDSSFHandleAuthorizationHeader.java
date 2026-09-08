package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.openid.ssf.variant.SsfAuthMode;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.time.Instant;

public class OIDSSFHandleAuthorizationHeader extends AbstractOIDSSFHandleReceiverRequest {

	@Override
	public Environment evaluate(Environment env) {

		JsonObject headersParams = env.getElementFromObject("incoming_request", "headers").getAsJsonObject();
		JsonElement authorizationHeaderEl = headersParams.get("authorization");

		JsonObject authResult = new JsonObject();
		env.putObject("ssf", "auth_result", authResult);

		// CAEPIOP 2.7.2: a transmitter MUST NOT accept access tokens via the URI query
		// parameter mechanism of RFC 6750 2.3. The emulated transmitter therefore rejects
		// them, and records the attempt so receiver tests can flag it.
		JsonElement queryParamsEl = env.getElementFromObject("incoming_request", "query_string_params");
		if (queryParamsEl != null && queryParamsEl.isJsonObject() && queryParamsEl.getAsJsonObject().has("access_token")) {
			authResult.addProperty("access_token_in_query", true);
			log("Request carried an access_token URI query parameter, which must not be accepted (CAEP Interop Profile 2.7.2, RFC 6750 2.3)");
			return unauthorized(env, authResult, "Access tokens must be sent in the Authorization header, not as a URI query parameter");
		}

		if (authorizationHeaderEl == null) {
			return unauthorized(env, authResult, "Missing authorization header in request");
		}

		String authorizationHeader = OIDFJSON.getString(authorizationHeaderEl);

		// DYNAMIC mode: validate the bearer token against the tokens issued by the
		// emulated authorization server (the /token endpoint). STATIC mode: compare
		// against the pre-shared transmitter access token.
		if (SsfAuthMode.DYNAMIC.name().equals(env.getString("ssf", "auth_mode"))) {
			return evaluateDynamic(env, authResult, authorizationHeader);
		}

		return evaluateStatic(env, authResult, authorizationHeader);
	}

	protected Environment evaluateStatic(Environment env, JsonObject authResult, String authorizationHeader) {

		String transmitterToken = env.getString("ssf", "transmitter_access_token");
		String expectedAuthorizationHeader = "Bearer " + transmitterToken;

		if (!expectedAuthorizationHeader.equals(authorizationHeader)) {
			log("Invalid Authorization header present in request", args("authorization_header", authorizationHeader,
				"expected_authorization_header", expectedAuthorizationHeader));
			return unauthorized(env, authResult, "Invalid authorization header in request");
		}

		logSuccess("Found valid Authorization header in request", args("authorization_header", authorizationHeader));
		return env;
	}

	protected Environment evaluateDynamic(Environment env, JsonObject authResult, String authorizationHeader) {

		if (!authorizationHeader.startsWith("Bearer ")) {
			return unauthorized(env, authResult, "Authorization header is not a Bearer token");
		}

		String token = authorizationHeader.substring("Bearer ".length());

		JsonElement issuedTokensEl = env.getElementFromObject("ssf", "issued_tokens");
		JsonObject issuedTokens = issuedTokensEl != null && issuedTokensEl.isJsonObject()
			? issuedTokensEl.getAsJsonObject() : new JsonObject();

		JsonElement tokenRecordEl = issuedTokens.get(token);
		if (tokenRecordEl == null || !tokenRecordEl.isJsonObject()) {
			log("Bearer token is not recognised", args("authorization_header", authorizationHeader));
			return unauthorized(env, authResult, "Bearer token is not recognised");
		}

		JsonObject tokenRecord = tokenRecordEl.getAsJsonObject();
		long expiresAt = OIDFJSON.getLong(tokenRecord.get("expires_at"));
		if (Instant.now().getEpochSecond() >= expiresAt) {
			log("Bearer token has expired", args("token_record", tokenRecord));
			return unauthorized(env, authResult, "Bearer token has expired");
		}

		JsonElement scopeEl = tokenRecord.get("scope");
		if (scopeEl != null) {
			// Stashed for per-operation scope enforcement (slice 3).
			env.putString("ssf", "current_token_scope", OIDFJSON.getString(scopeEl));
		}

		logSuccess("Found valid Bearer token in request", args("token_record", tokenRecord));
		return env;
	}

	protected Environment unauthorized(Environment env, JsonObject authResult, String description) {
		authResult.add("error", createErrorObj("unauthorized", description));
		authResult.addProperty("status_code", 401);
		// RFC 6750 3 / 3.1: a 401 carries a WWW-Authenticate challenge with the error code
		authResult.addProperty("www_authenticate", "Bearer error=\"invalid_token\", error_description=\"" + description + "\"");
		log(description);
		return env;
	}
}
