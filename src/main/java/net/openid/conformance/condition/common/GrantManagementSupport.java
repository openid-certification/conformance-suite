package net.openid.conformance.condition.common;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractCallProtectedResourceWithBearerToken;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.http.HttpMethod;

public class GrantManagementSupport {

	// Environment key under which the grant_id string is stored
	public static final String GRANT_ID_KEY = "grant_id";

	// Environment key under which the grant management endpoint URL (including grant_id path segment) is stored
	public static final String GRANT_MANAGEMENT_URL_KEY = "grant_management_url";

	// Environment key under which the full grant management endpoint response is stored
	public static final String GRANT_MANAGEMENT_RESPONSE_KEY = "grant_management_response_full";


	// ----  OP (server) test conditions  ----

	/**
	 * Appends grant_management_query and grant_management_revoke to the scope in the authorization endpoint request.
	 * Required so the resulting access token can be used to query and revoke grants.
	 */
	public static class AddGrantManagementScopesToAuthorizationRequest extends AbstractCondition {

		@Override
		@PreEnvironment(required = "authorization_endpoint_request")
		@PostEnvironment(required = "authorization_endpoint_request")
		public Environment evaluate(Environment env) {
			JsonObject req = env.getObject("authorization_endpoint_request");
			String existingScope = req.has("scope") ? OIDFJSON.getString(req.get("scope")) : "";
			String newScope = existingScope.isEmpty()
				? "grant_management_query grant_management_revoke"
				: existingScope + " grant_management_query grant_management_revoke";
			req.addProperty("scope", newScope);
			logSuccess("Added grant_management_query and grant_management_revoke scopes to authorization request", args("scope", newScope));
			return env;
		}
	}

	/**
	 * Adds grant_management_action=create to the authorization endpoint request.
	 */
	public static class AddGrantManagementActionCreateToAuthorizationRequest extends AbstractCondition {

		@Override
		@PreEnvironment(required = "authorization_endpoint_request")
		@PostEnvironment(required = "authorization_endpoint_request")
		public Environment evaluate(Environment env) {
			JsonObject req = env.getObject("authorization_endpoint_request");
			req.addProperty("grant_management_action", "create");
			logSuccess("Added grant_management_action=create to authorization request", req);
			return env;
		}
	}

	/**
	 * Adds grant_management_action=update to the authorization endpoint request.
	 * Also requires a grant_id to be present in the environment.
	 */
	public static class AddGrantManagementActionMergeToAuthorizationRequest extends AbstractCondition {

		@Override
		@PreEnvironment(required = "authorization_endpoint_request")
		@PostEnvironment(required = "authorization_endpoint_request")
		public Environment evaluate(Environment env) {
			JsonObject req = env.getObject("authorization_endpoint_request");
			req.addProperty("grant_management_action", "merge");
			logSuccess("Added grant_management_action=merge to authorization request", req);
			return env;
		}
	}

	/**
	 * Adds grant_management_action=replace to the authorization endpoint request.
	 * Also requires a grant_id to be present in the environment.
	 */
	public static class AddGrantManagementActionReplaceToAuthorizationRequest extends AbstractCondition {

		@Override
		@PreEnvironment(required = "authorization_endpoint_request")
		@PostEnvironment(required = "authorization_endpoint_request")
		public Environment evaluate(Environment env) {
			JsonObject req = env.getObject("authorization_endpoint_request");
			req.addProperty("grant_management_action", "replace");
			logSuccess("Added grant_management_action=replace to authorization request", req);
			return env;
		}
	}

	/**
	 * Adds the grant_id stored in the environment to the authorization endpoint request.
	 * Must be called after ExtractGrantIdFromTokenResponse.
	 */
	public static class AddGrantIdToAuthorizationRequest extends AbstractCondition {

		@Override
		@PreEnvironment(required = "authorization_endpoint_request", strings = GRANT_ID_KEY)
		@PostEnvironment(required = "authorization_endpoint_request")
		public Environment evaluate(Environment env) {
			String grantId = env.getString(GRANT_ID_KEY);
			JsonObject req = env.getObject("authorization_endpoint_request");
			req.addProperty("grant_id", grantId);
			logSuccess("Added grant_id to authorization request", args("grant_id", grantId));
			return env;
		}
	}

	/**
	 * Extracts and validates grant_id from the token endpoint response.
	 * Fails if grant_id is missing or has insufficient entropy.
	 */
	public static class ExtractGrantIdFromTokenResponse extends AbstractCondition {

		@Override
		@PreEnvironment(required = "token_endpoint_response")
		@PostEnvironment(strings = GRANT_ID_KEY)
		public Environment evaluate(Environment env) {
			String grantId = env.getString("token_endpoint_response", "grant_id");
			if (grantId == null || grantId.isEmpty()) {
				throw error("grant_id missing from token endpoint response",
					args("token_endpoint_response", env.getObject("token_endpoint_response")));
			}
			// GM spec requires grant_id to be a URL-safe string with high entropy (at least 128 bits)
			if (grantId.length() < 20) {
				throw error("grant_id appears to have insufficient entropy (too short)",
					args("grant_id", grantId, "minimum_length", 20));
			}
			env.putString(GRANT_ID_KEY, grantId);
			logSuccess("Extracted grant_id from token endpoint response", args("grant_id", grantId));
			return env;
		}
	}

	/**
	 * Builds the grant management endpoint URL by appending grant_id to the grant_management_endpoint
	 * from server metadata. Stores the result as grant_management_url.
	 */
	public static class SetGrantManagementEndpointUrl extends AbstractCondition {

		@Override
		@PreEnvironment(required = "server", strings = GRANT_ID_KEY)
		@PostEnvironment(strings = GRANT_MANAGEMENT_URL_KEY)
		public Environment evaluate(Environment env) {
			String endpoint = env.getString("server", "grant_management_endpoint");
			if (endpoint == null || endpoint.isEmpty()) {
				throw error("grant_management_endpoint missing from server metadata");
			}
			String grantId = env.getString(GRANT_ID_KEY);
			String url = endpoint.endsWith("/") ? endpoint + grantId : endpoint + "/" + grantId;
			env.putString(GRANT_MANAGEMENT_URL_KEY, url);
			logSuccess("Built grant management endpoint URL", args("url", url));
			return env;
		}
	}

	/**
	 * Checks that grant_management_endpoint is present in server metadata and is an HTTPS URL.
	 */
	public static class CheckDiscoveryForGrantManagementEndpoint extends AbstractCondition {

		@Override
		@PreEnvironment(required = "server")
		public Environment evaluate(Environment env) {
			String endpoint = env.getString("server", "grant_management_endpoint");
			if (endpoint == null || endpoint.isEmpty()) {
				throw error("grant_management_endpoint missing from server metadata",
					args("server", env.getObject("server")));
			}
			if (!endpoint.startsWith("https://")) {
				throw error("grant_management_endpoint must be an HTTPS URL",
					args("grant_management_endpoint", endpoint));
			}
			logSuccess("grant_management_endpoint found in server metadata", args("grant_management_endpoint", endpoint));
			return env;
		}
	}

	/**
	 * Checks that grant_management_actions_supported contains at least create, query, revoke.
	 */
	public static class CheckDiscoveryForGrantManagementActionsSupported extends AbstractCondition {

		@Override
		@PreEnvironment(required = "server")
		public Environment evaluate(Environment env) {
			com.google.gson.JsonArray supported = getJsonArrayFromEnvironment(env, "server", "grant_management_actions_supported",
				"grant_management_actions_supported in server metadata", true);
			boolean hasCreate = false;
			boolean hasQuery = false;
			boolean hasRevoke = false;
			for (JsonElement element : supported) {
				String action = OIDFJSON.getString(element);
				if ("create".equals(action)) { hasCreate = true; }
				if ("query".equals(action)) { hasQuery = true; }
				if ("revoke".equals(action)) { hasRevoke = true; }
			}
			if (!hasCreate || !hasQuery || !hasRevoke) {
				throw error("grant_management_actions_supported must contain at least create, query, and revoke",
					args("grant_management_actions_supported", supported));
			}
			logSuccess("grant_management_actions_supported contains required actions", args("grant_management_actions_supported", supported));
			return env;
		}
	}

	/**
	 * Calls the grant management endpoint with HTTP GET (query).
	 * Uses the bearer access token for authorization.
	 * Stores the full response as grant_management_response_full.
	 */
	public static class CallGrantManagementEndpointQuery extends AbstractCallProtectedResourceWithBearerToken {

		@Override
		protected boolean treatAllHttpStatusAsSuccess() {
			return true;
		}

		@Override
		protected boolean requireJsonResponseBody() {
			return true;
		}

		@Override
		protected String getUri(Environment env) {
			String url = env.getString(GRANT_MANAGEMENT_URL_KEY);
			if (url == null || url.isEmpty()) {
				throw error("grant_management_url not set in environment");
			}
			return url;
		}

		@Override
		protected HttpMethod getMethod(Environment env) {
			return HttpMethod.GET;
		}

		@Override
		@PreEnvironment(required = "access_token", strings = GRANT_MANAGEMENT_URL_KEY)
		@PostEnvironment(required = GRANT_MANAGEMENT_RESPONSE_KEY)
		public Environment evaluate(Environment env) {
			return callProtectedResource(env);
		}

		@Override
		protected Environment handleClientResponse(Environment env, JsonObject responseCode, String responseBody, JsonObject responseHeaders, JsonObject fullResponse) {
			env.putObject(GRANT_MANAGEMENT_RESPONSE_KEY, fullResponse);
			logSuccess("Got a response from the grant management endpoint (query)", fullResponse);
			return env;
		}
	}

	public static class CallGrantManagementEndpointFailureQuery extends CallGrantManagementEndpointQuery{
		@Override
		protected boolean requireJsonResponseBody() {
			return false;
		}
	}

	/**
	 * Calls the grant management endpoint with HTTP DELETE (revoke).
	 * Uses the bearer access token for authorization.
	 * Stores the full response as grant_management_response_full.
	 */
	public static class CallGrantManagementEndpointRevoke extends AbstractCallProtectedResourceWithBearerToken {

		@Override
		protected boolean treatAllHttpStatusAsSuccess() {
			return true;
		}

		@Override
		protected String getUri(Environment env) {
			String url = env.getString(GRANT_MANAGEMENT_URL_KEY);
			if (url == null || url.isEmpty()) {
				throw error("grant_management_url not set in environment");
			}
			return url;
		}

		@Override
		protected HttpMethod getMethod(Environment env) {
			return HttpMethod.DELETE;
		}

		@Override
		@PreEnvironment(required = "access_token", strings = GRANT_MANAGEMENT_URL_KEY)
		@PostEnvironment(required = GRANT_MANAGEMENT_RESPONSE_KEY)
		public Environment evaluate(Environment env) {
			return callProtectedResource(env);
		}

		@Override
		protected Environment handleClientResponse(Environment env, JsonObject responseCode, String responseBody, JsonObject responseHeaders, JsonObject fullResponse) {
			env.putObject(GRANT_MANAGEMENT_RESPONSE_KEY, fullResponse);
			logSuccess("Got a response from the grant management endpoint (revoke)", fullResponse);
			return env;
		}
	}

	/**
	 * Verifies that the grant management endpoint returned HTTP 200.
	 */
	public static class EnsureGrantManagementQuerySucceeded extends AbstractCondition {

		@Override
		@PreEnvironment(required = GRANT_MANAGEMENT_RESPONSE_KEY)
		public Environment evaluate(Environment env) {
			int code = env.getInteger(GRANT_MANAGEMENT_RESPONSE_KEY, "status");
			if (code != 200) {
				throw error("Expected HTTP 200 from grant management query endpoint",
					args("http_status", code));
			}
			logSuccess("Grant management query endpoint returned HTTP 200");
			return env;
		}
	}

	/**
	 * Validates the body of a successful grant management query response.
	 * The response should contain at least one of: scopes, claims, authorization_details.
	 */
	public static class ValidateGrantManagementQueryResponse extends AbstractCondition {

		@Override
		@PreEnvironment(required = GRANT_MANAGEMENT_RESPONSE_KEY)
		public Environment evaluate(Environment env) {
			JsonElement bodyElement = env.getElementFromObject(GRANT_MANAGEMENT_RESPONSE_KEY, "body_json");

			if (bodyElement == null || !bodyElement.isJsonObject()) {
				throw error("Grant management query response body is missing or not a JSON object");
			}
			JsonObject body = bodyElement.getAsJsonObject();

			boolean hasScopes = body.has("scopes");
			boolean hasClaims = body.has("claims");
			boolean hasAuthDetails = body.has("authorization_details");

			if (!hasScopes && !hasClaims && !hasAuthDetails) {
				throw error("Grant management query response must contain at least one of: scopes, claims, authorization_details",
					args("body", body));
			}

			logSuccess("Grant management query response is valid", args("body", body));
			return env;
		}
	}

	/**
	 * Verifies that the grant management endpoint returned HTTP 204 (revoke success).
	 */
	public static class EnsureGrantManagementRevokeSucceeded extends AbstractCondition {

		@Override
		@PreEnvironment(required = GRANT_MANAGEMENT_RESPONSE_KEY)
		public Environment evaluate(Environment env) {
			int code = env.getInteger(GRANT_MANAGEMENT_RESPONSE_KEY, "status");
			if (code != 204) {
				throw error("Expected HTTP 204 from grant management revoke endpoint",
					args("http_status", code));
			}
			logSuccess("Grant management revoke endpoint returned HTTP 204");
			return env;
		}
	}

	/**
	 * Verifies that the grant management endpoint returned HTTP 404 (e.g. after revoke, or for unknown grant).
	 */
	public static class EnsureGrantManagementEndpointReturns404 extends AbstractCondition {

		@Override
		@PreEnvironment(required = GRANT_MANAGEMENT_RESPONSE_KEY)
		public Environment evaluate(Environment env) {
			int code = env.getInteger(GRANT_MANAGEMENT_RESPONSE_KEY, "status");
			if (code != 404) {
				throw error("Expected HTTP 404 from grant management endpoint",
					args("http_status", code));
			}
			logSuccess("Grant management endpoint returned HTTP 404 as expected");
			return env;
		}
	}

	/**
	 * Verifies that the grant management endpoint returned HTTP 403 (wrong client).
	 */
	public static class EnsureGrantManagementEndpointReturns403 extends AbstractCondition {

		@Override
		@PreEnvironment(required = GRANT_MANAGEMENT_RESPONSE_KEY)
		public Environment evaluate(Environment env) {
			int code = env.getInteger(GRANT_MANAGEMENT_RESPONSE_KEY, "status");
			if (code != 403) {
				throw error("Expected HTTP 403 from grant management endpoint",
					args("http_status", code));
			}
			logSuccess("Grant management endpoint returned HTTP 403 as expected");
			return env;
		}
	}

	/**
	 * Verifies that the grant management endpoint returned HTTP 403 or 404 (wrong client).
	 * Either is acceptable per the spec.
	 */
	public static class EnsureGrantManagementEndpointReturns403Or404 extends AbstractCondition {

		@Override
		@PreEnvironment(required = GRANT_MANAGEMENT_RESPONSE_KEY)
		public Environment evaluate(Environment env) {
			int code = env.getInteger(GRANT_MANAGEMENT_RESPONSE_KEY, "status");
			if (code != 403 && code != 404) {
				throw error("Expected HTTP 403 or 404 from grant management endpoint when accessed by wrong client",
					args("http_status", code));
			}
			logSuccess("Grant management endpoint returned HTTP " + code + " as expected for wrong-client access");
			return env;
		}
	}

	/**
	 * Validates that the authorization endpoint returned invalid_grant_id error.
	 */
	public static class EnsureAuthorizationEndpointRejectsInvalidGrantId extends AbstractCondition {

		@Override
		@PreEnvironment(required = "authorization_endpoint_response")
		public Environment evaluate(Environment env) {
			String error = env.getString("authorization_endpoint_response", "error");
			if (!"invalid_grant_id".equals(error)) {
				throw error("Expected invalid_grant_id error from authorization endpoint",
					args("error", error, "authorization_endpoint_response", env.getObject("authorization_endpoint_response")));
			}
			logSuccess("Authorization endpoint returned invalid_grant_id error as expected");
			return env;
		}
	}

	/**
	 * Validates that the PAR endpoint response body contains invalid_grant_id error.
	 */
	public static class EnsurePAREndpointRejectsInvalidGrantId extends AbstractCondition {

		@Override
		@PreEnvironment(required = "endpoint_response")
		public Environment evaluate(Environment env) {
			String error = env.getString("endpoint_response", "body_json.error");
			if (!"invalid_grant_id".equals(error)) {
				throw error("Expected invalid_grant_id error from PAR endpoint",
					args("error", error, "endpoint_response", env.getObject("endpoint_response")));
			}
			logSuccess("PAR endpoint returned invalid_grant_id error as expected");
			return env;
		}
	}


	// ----  RP (client) test conditions  ----

	/**
	 * Creates an invalid_grant_id error response for the PAR endpoint.
	 * Sets par_endpoint_response, par_endpoint_response_headers, and par_endpoint_response_http_status.
	 * Used in client (RP) tests to simulate the AS rejecting an invalid grant_id at the PAR endpoint.
	 */
	public static class CreatePAREndpointInvalidGrantIdErrorResponse extends AbstractCondition {

		@Override
		@PostEnvironment(required = {"par_endpoint_response", "par_endpoint_response_headers"})
		public Environment evaluate(Environment env) {
			JsonObject errorResponse = new JsonObject();
			errorResponse.addProperty("error", "invalid_grant_id");
			errorResponse.addProperty("error_description", "The provided grant_id is unknown or invalid");
			env.putObject("par_endpoint_response", errorResponse);

			JsonObject headers = new JsonObject();
			headers.addProperty("Content-Type", "application/json");
			env.putObject("par_endpoint_response_headers", headers);

			env.putInteger("par_endpoint_response_http_status", 400);

			logSuccess("Created invalid_grant_id error response for PAR endpoint", errorResponse);
			return env;
		}
	}

	/**
	 * Generates a random grant_id and adds it to the token endpoint response.
	 * Used by the suite when acting as AS in client (RP) tests.
	 */
	public static class AddGrantIdToTokenEndpointResponse extends AbstractCondition {

		@Override
		@PreEnvironment(required = "token_endpoint_response")
		@PostEnvironment(required = "token_endpoint_response", strings = GRANT_ID_KEY)
		public Environment evaluate(Environment env) {
			String grantId = RandomStringUtils.secure().nextAlphanumeric(32);
			JsonObject tokenResponse = env.getObject("token_endpoint_response");
			tokenResponse.addProperty("grant_id", grantId);
			env.putString(GRANT_ID_KEY, grantId);
			logSuccess("Added grant_id to token endpoint response", args("grant_id", grantId));
			return env;
		}
	}

	/**
	 * Checks that the client's authorization request contains grant_management_action=update
	 * with a valid grant_id matching the one previously issued.
	 */
	public static class EnsureAuthorizationRequestContainsGrantManagementActionUpdateWithGrantId extends AbstractCondition {

		@Override
		@PreEnvironment(required = "authorization_endpoint_http_request_params", strings = GRANT_ID_KEY)
		public Environment evaluate(Environment env) {
			String action = env.getString("authorization_endpoint_http_request_params", "grant_management_action");
			String grantId = env.getString("authorization_endpoint_http_request_params", "grant_id");
			String expectedGrantId = env.getString(GRANT_ID_KEY);

			if (!"update".equals(action)) {
				throw error("Expected grant_management_action=update in authorization request",
					args("grant_management_action", action));
			}
			if (grantId == null || grantId.isEmpty()) {
				throw error("grant_id missing from authorization request with grant_management_action=update");
			}
			if (!expectedGrantId.equals(grantId)) {
				throw error("grant_id in authorization request does not match previously issued grant_id",
					args("expected", expectedGrantId, "actual", grantId));
			}
			logSuccess("Authorization request contains valid grant_management_action=update with grant_id",
				args("grant_id", grantId));
			return env;
		}
	}

	/**
	 * Sets the authorization endpoint error response to invalid_grant_id.
	 * Used by the suite when acting as AS in client (RP) tests.
	 */
	public static class SetInvalidGrantIdErrorForAuthorizationEndpoint extends AbstractCondition {

		@Override
		public Environment evaluate(Environment env) {
			JsonObject errorResponse = new JsonObject();
			errorResponse.addProperty("error", "invalid_grant_id");
			errorResponse.addProperty("error_description", "The provided grant_id is unknown or invalid");
			env.putObject("authorization_endpoint_error_response", errorResponse);
			logSuccess("Set invalid_grant_id error for authorization endpoint response");
			return env;
		}
	}
}
