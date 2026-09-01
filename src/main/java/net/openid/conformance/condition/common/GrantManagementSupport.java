package net.openid.conformance.condition.common;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCheckForUnexpectedSchemaProperties;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.AbstractJsonSchemaBasedValidation;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractCallProtectedResourceWithBearerToken;
import net.openid.conformance.condition.client.AbstractCheckEndpointContentTypeReturned;
import net.openid.conformance.condition.client.AddDpopHeaderForResourceEndpointRequest;
import net.openid.conformance.condition.client.CreateDpopClaims;
import net.openid.conformance.condition.client.CreateDpopHeader;
import net.openid.conformance.condition.client.EnsureDpopNonceContainsAllowedCharactersOnly;
import net.openid.conformance.condition.client.ExtractGrantedScopeFromTokenEndpointResponse;
import net.openid.conformance.condition.client.SetDpopAccessTokenHash;
import net.openid.conformance.condition.client.SetDpopProofNonceForResourceEndpoint;
import net.openid.conformance.condition.client.SignDpopProof;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.http.DpopNonceResponseHeader;
import net.openid.conformance.util.http.WwwAuthenticateHeaderValueParser;
import net.openid.conformance.util.validation.JsonSchemaValidationInput;
import net.openid.conformance.util.validation.JsonSchemaValidationResult;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

public class GrantManagementSupport {

	// Environment key under which the grant_id string is stored
	public static final String GRANT_ID_KEY = "grant_id";

	/** The grant_id issued by an earlier flow, kept so a merge or replace can be checked against it. */
	public static final String PREVIOUS_GRANT_ID_KEY = "previous_grant_id";

	// Environment key under which the grant management endpoint URL (including grant_id path segment) is stored
	public static final String GRANT_MANAGEMENT_URL_KEY = "grant_management_url";

	// Environment key under which the full grant management endpoint response is stored
	public static final String GRANT_MANAGEMENT_RESPONSE_KEY = "grant_management_response_full";

	// Environment key set when the grant management endpoint asked us to repeat the request with a DPoP nonce
	public static final String GRANT_MANAGEMENT_DPOP_NONCE_ERROR_KEY = "grant_management_dpop_nonce_error";

	// Environment key holding the grant management action the invalid grant_id test picked (merge or replace),
	// left unset when the AS advertises neither
	public static final String SELECTED_GRANT_MANAGEMENT_ACTION_KEY = "selected_grant_management_action";

	// The scopes GM 6.1 defines for accessing the grant management API itself. They authorize calls to the
	// grant management endpoint; they are not permissions held within the grant, so they must not be
	// expected to appear in the 'scopes' of a query response.
	public static final Set<String> GRANT_MANAGEMENT_API_SCOPES =
		Set.of("grant_management_query", "grant_management_revoke");


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
			String apiScopes = String.join(" ", new TreeSet<>(GRANT_MANAGEMENT_API_SCOPES));
			String newScope = existingScope.isEmpty() ? apiScopes : existingScope + " " + apiScopes;
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
	 * Adds grant_management_action=merge to the authorization endpoint request.
	 * The grant_id itself is added separately by AddGrantIdToAuthorizationRequest.
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
	 * The grant_id itself is added separately by AddGrantIdToAuthorizationRequest.
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
	 * Adds whichever of merge/replace the AS advertises in grant_management_actions_supported as the
	 * grant_management_action, storing the choice under {@link #SELECTED_GRANT_MANAGEMENT_ACTION_KEY}.
	 *
	 * <p>Used by the invalid grant_id test, which needs an action that takes a grant_id but is not itself
	 * the subject of the test. GM 7.1 makes both merge and replace optional, so the module must not assume
	 * either one: an AS that supports only create/query/revoke would correctly answer invalid_request
	 * ("the AS does not support a grant management action requested by the client", GM 5.4) rather than
	 * invalid_grant_id. Callers check {@link #SELECTED_GRANT_MANAGEMENT_ACTION_KEY} afterwards and skip the
	 * test when neither action is available.
	 */
	public static class SelectGrantManagementActionTakingAGrantId extends AbstractCondition {

		@Override
		@PreEnvironment(required = { "server", "authorization_endpoint_request" })
		public Environment evaluate(Environment env) {
			JsonArray supported = getJsonArrayFromEnvironment(env, "server", "grant_management_actions_supported",
				"grant_management_actions_supported in server metadata", true);

			Set<String> advertised = new LinkedHashSet<>();
			for (JsonElement element : supported) {
				advertised.add(OIDFJSON.getString(element));
			}

			// merge is preferred purely for consistency with the merge module; either action carries a grant_id
			String action = advertised.contains("merge") ? "merge" : (advertised.contains("replace") ? "replace" : null);
			if (action == null) {
				logSuccess("The authorization server advertises neither 'merge' nor 'replace', so there is no"
						+ " grant management action that carries a grant_id and this test cannot be run",
					args("grant_management_actions_supported", supported));
				return env;
			}

			env.putString(SELECTED_GRANT_MANAGEMENT_ACTION_KEY, action);
			env.getObject("authorization_endpoint_request").addProperty("grant_management_action", action);
			logSuccess("Added grant_management_action=" + action + " to authorization request",
				args("grant_management_action", action, "grant_management_actions_supported", supported));
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
	 * Extracts grant_id from the token endpoint response.
	 * GM 5.5 makes grant_id OPTIONAL in general, but the AS "must return a grant_id if the
	 * grant_management_action request parameter is provided and the specified action is valid and
	 * supported" - every grant management test sends an action, so a missing grant_id is a failure.
	 * The quality of the value is checked separately by {@link CheckGrantIdIsUrlSafe} and
	 * {@link CheckGrantIdHasSufficientEntropy}.
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
			env.putString(GRANT_ID_KEY, grantId);
			logSuccess("Extracted grant_id from token endpoint response", args("grant_id", grantId));
			return env;
		}
	}

	/**
	 * Replaces the stored grant_id with one the AS cannot have issued, for the tests that check an unknown
	 * grant is rejected. The value is random rather than derived from the clock so that it is unguessable
	 * and cannot collide with a real identifier, and it uses only characters GM 5.5 allows, so the only
	 * thing wrong with it is that no such grant exists.
	 */
	public static class CreateGrantIdThatDoesNotExist extends AbstractCondition {

		@Override
		@PostEnvironment(strings = GRANT_ID_KEY)
		public Environment evaluate(Environment env) {
			String grantId = RandomStringUtils.secure().nextAlphanumeric(32);
			env.putString(GRANT_ID_KEY, grantId);
			logSuccess("Created a grant_id that the authorization server cannot have issued",
				args("grant_id", grantId));
			return env;
		}
	}

	/**
	 * Remembers the grant_id the AS issued, so that a later flow can check the AS did not silently mint a
	 * new grant. {@link ExtractGrantIdFromTokenResponse} overwrites {@link #GRANT_ID_KEY} on every token
	 * response, so the value has to be copied aside before the next authorization flow runs.
	 */
	public static class StoreGrantIdForComparison extends AbstractCondition {

		@Override
		@PreEnvironment(strings = GRANT_ID_KEY)
		@PostEnvironment(strings = PREVIOUS_GRANT_ID_KEY)
		public Environment evaluate(Environment env) {
			String grantId = env.getString(GRANT_ID_KEY);
			env.putString(PREVIOUS_GRANT_ID_KEY, grantId);
			logSuccess("Recorded the issued grant_id so it can be compared against the one returned by the next flow",
				args("grant_id", grantId));
			return env;
		}
	}

	/**
	 * Verifies that a merge or replace updated the grant the client named rather than creating a new one.
	 *
	 * <p>GM 5.2 defines both actions as operating on the existing grant - merge "will merge the permissions
	 * ... with those which already exist within the grant", replace "will change the grant to be ONLY the
	 * permissions requested". The client identified that grant by sending its grant_id, so an AS that
	 * answers with a different grant_id has left the named grant untouched and created a second one, which
	 * is not the behaviour either action describes.
	 *
	 * <p>This is an interpretation, not a normative requirement: GM never states that merge/replace return
	 * the same grant_id, and an AS that rotates the identifier of the same logical grant is not
	 * demonstrably non-conformant. The interpretation is spelled out in the failure message and callers
	 * invoke this with WARNING severity so the tester can push back.
	 */
	public static class EnsureGrantIdIsUnchanged extends AbstractCondition {

		@Override
		@PreEnvironment(strings = { GRANT_ID_KEY, PREVIOUS_GRANT_ID_KEY })
		public Environment evaluate(Environment env) {
			String previous = env.getString(PREVIOUS_GRANT_ID_KEY);
			String current = env.getString(GRANT_ID_KEY);

			if (!previous.equals(current)) {
				throw error("The authorization server returned a different grant_id, so it appears to have created"
						+ " a new grant instead of updating the one the request named. GM 5.2 defines both 'merge'"
						+ " and 'replace' as acting on the existing grant identified by the grant_id in the"
						+ " request. Note that this is the conformance suite's interpretation: GM does not state"
						+ " that the grant_id is stable across merge/replace, so an authorization server that"
						+ " rotates the identifier of the same logical grant is reported as a warning.",
					args("grant_id_in_request", previous, "grant_id_in_token_response", current));
			}

			logSuccess("The authorization server returned the same grant_id, so it updated the existing grant",
				args("grant_id", current));
			return env;
		}
	}

	/**
	 * GM 5.5 says the grant_id is "a URL safe string", without defining the term. The value forms the last
	 * path segment of the grant resource URL, so the check accepts everything RFC 3986 allows unencoded in
	 * a path segment (pchar: unreserved / sub-delims / ":" / "@"); anything outside that must be percent
	 * encoded, which {@link SetGrantManagementEndpointUrl} does defensively anyway.
	 *
	 * <p>Because the spec does not define "URL safe", callers invoke this with WARNING severity, separately
	 * from the length heuristic in {@link CheckGrantIdHasSufficientEntropy}.
	 */
	public static class CheckGrantIdIsUrlSafe extends AbstractCondition {

		// RFC 3986 pchar, minus pct-encoded: unreserved / sub-delims / ":" / "@"
		private static final Pattern URL_SAFE = Pattern.compile("^[A-Za-z0-9._~!$&'()*+,;=:@-]+$");

		@Override
		@PreEnvironment(strings = GRANT_ID_KEY)
		public Environment evaluate(Environment env) {
			String grantId = env.getString(GRANT_ID_KEY);

			if (!URL_SAFE.matcher(grantId).matches()) {
				throw error("grant_id contains characters that have to be percent encoded to appear in a URL path"
						+ " segment. GM 5.5 says the grant_id is a URL safe string, but does not define the term,"
						+ " so this is reported as a warning; the conformance suite percent encodes the value when"
						+ " building the grant resource URL.",
					args("grant_id", grantId));
			}

			logSuccess("grant_id is a URL safe string", args("grant_id", grantId));
			return env;
		}
	}

	/**
	 * GM 5.5 also requires the grant_id to have "sufficient entropy to prevent guessing", but states no
	 * minimum length, so the length check here is only a rule of thumb - callers must invoke this with
	 * WARNING severity rather than failing an AS whose identifiers are shorter than the suite's heuristic
	 * but still unguessable.
	 */
	public static class CheckGrantIdHasSufficientEntropy extends AbstractCondition {

		// a rule of thumb, not a spec requirement: ~120 bits when the value is base64url encoded random data
		private static final int SUGGESTED_MINIMUM_LENGTH = 20;

		@Override
		@PreEnvironment(strings = GRANT_ID_KEY)
		public Environment evaluate(Environment env) {
			String grantId = env.getString(GRANT_ID_KEY);

			if (grantId.length() < SUGGESTED_MINIMUM_LENGTH) {
				throw error("grant_id may not have sufficient entropy to prevent guessing, as required by GM 5.5. This is a warning as the specification does not state a minimum length; check that the value is generated from a cryptographically secure source",
					args("grant_id", grantId, "length", grantId.length(), "suggested_minimum_length", SUGGESTED_MINIMUM_LENGTH));
			}

			logSuccess("grant_id is of a plausible length", args("grant_id", grantId));
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
			// The grant management endpoint is always called with a sender constrained access token, so when
			// mTLS is in use we must honour mtls_endpoint_aliases. AddMTLSEndpointAliasesToEnvironment has
			// flattened every *_endpoint (aliased value preferred) into the root of the environment; this
			// follows the same pattern as CallPAREndpoint and the token endpoint conditions.
			String endpoint = null;
			if (env.containsObject("mutual_tls_authentication")) {
				endpoint = env.getString("grant_management_endpoint");
			}
			if (endpoint == null || endpoint.isEmpty()) {
				endpoint = env.getString("server", "grant_management_endpoint");
			}
			if (endpoint == null || endpoint.isEmpty()) {
				throw error("grant_management_endpoint missing from server metadata");
			}
			String grantId = env.getString(GRANT_ID_KEY);
			// GM 5.5 says the grant_id is a URL safe string, but an AS that gets that wrong must not be
			// able to redirect our request somewhere else, so the value is encoded as a single path segment
			String encodedGrantId = UriUtils.encodePathSegment(grantId, StandardCharsets.UTF_8);
			String url = endpoint.endsWith("/") ? endpoint + encodedGrantId : endpoint + "/" + encodedGrantId;
			env.putString(GRANT_MANAGEMENT_URL_KEY, url);
			logSuccess("Built grant management endpoint URL", args("url", url, "grant_id", grantId));
			return env;
		}
	}

	/**
	 * Checks that grant_management_endpoint is present in server metadata and is an HTTPS URL.
	 * The metadata member is OPTIONAL per GM 7.1, but GM 6.2/6.3 make it the only way for a client to
	 * learn the grant resource URL, so an AS under a grant management test plan must advertise it:
	 * callers invoke this with FAILURE severity, as they do for
	 * {@link CheckDiscoveryForGrantManagementActionsSupported}.
	 */
	public static class CheckDiscoveryForGrantManagementEndpoint extends AbstractCondition {

		@Override
		@PreEnvironment(required = "server")
		public Environment evaluate(Environment env) {
			String endpoint = env.getString("server", "grant_management_endpoint");
			if (endpoint == null || endpoint.isEmpty()) {
				throw error("grant_management_endpoint is missing from the server metadata. GM 7.1 marks the metadata member OPTIONAL, but this test plan was configured with grant management enabled, and GM 6.2/6.3 give the client no other way to find the grant resource URL",
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
	 * Checks that grant_management_actions_supported is present in server metadata.
	 *
	 * The metadata member is OPTIONAL in GM 7.1, but the same section says "if omitted, the AS does not
	 * support any grant management actions" - so under a plan that is testing grant management, omitting it
	 * is the server declaring that it does not do what it is being certified for. Callers therefore invoke
	 * this with FAILURE severity.
	 */
	public static class CheckDiscoveryForGrantManagementActionsSupported extends AbstractCondition {

		@Override
		@PreEnvironment(required = "server")
		public Environment evaluate(Environment env) {
			JsonArray supported = getJsonArrayFromEnvironment(env, "server", "grant_management_actions_supported",
				"grant_management_actions_supported in server metadata", true);
			logSuccess("grant_management_actions_supported found in server metadata",
				args("grant_management_actions_supported", supported));
			return env;
		}
	}

	/**
	 * Checks that grant_management_actions_supported covers the actions a test needs. GM 7.1 allows
	 * query, revoke, merge, replace and create.
	 */
	public abstract static class AbstractCheckGrantManagementActionsSupported extends AbstractCondition {

		protected abstract Set<String> requiredActions();

		@Override
		@PreEnvironment(required = "server")
		public Environment evaluate(Environment env) {
			JsonArray supported = getJsonArrayFromEnvironment(env, "server", "grant_management_actions_supported",
				"grant_management_actions_supported in server metadata", true);

			Set<String> advertised = new LinkedHashSet<>();
			for (JsonElement element : supported) {
				advertised.add(OIDFJSON.getString(element));
			}

			List<String> missing = requiredActions().stream()
				.filter(action -> !advertised.contains(action))
				.sorted()
				.toList();

			if (!missing.isEmpty()) {
				throw error("grant_management_actions_supported does not advertise every action this test needs",
					args("missing_actions", missing, "grant_management_actions_supported", supported));
			}

			logSuccess("grant_management_actions_supported advertises the actions this test needs",
				args("required_actions", requiredActions(), "grant_management_actions_supported", supported));
			return env;
		}
	}

	/**
	 * The actions every grant management test module exercises: creating a grant, then querying and
	 * revoking it through the grant management API.
	 */
	public static class CheckDiscoveryForGrantManagementActionsSupportedContainsRequiredActions extends AbstractCheckGrantManagementActionsSupported {
		@Override
		protected Set<String> requiredActions() {
			return Set.of("create", "query", "revoke");
		}
	}

	/** Used by the merge module, which the generic discovery check cannot require of every server. */
	public static class CheckGrantManagementActionsSupportedContainsMerge extends AbstractCheckGrantManagementActionsSupported {
		@Override
		protected Set<String> requiredActions() {
			return Set.of("merge");
		}
	}

	/** Used by the replace module, which the generic discovery check cannot require of every server. */
	public static class CheckGrantManagementActionsSupportedContainsReplace extends AbstractCheckGrantManagementActionsSupported {
		@Override
		protected Set<String> requiredActions() {
			return Set.of("replace");
		}
	}

	/**
	 * Base for the calls to the grant management endpoint (GM 6.3: [grant_management_endpoint]/[grant_id]).
	 *
	 * The grant management endpoint is a protected resource, so under DPoP sender constraining the request
	 * needs a DPoP proof as well as the 'DPoP' authorization scheme. The proof is built by
	 * {@link CreateGrantManagementQueryDpopProofSteps}/{@link CreateGrantManagementRevokeDpopProofSteps}
	 * into resource_endpoint_request_headers, which is why the headers are merged in below - without this
	 * the AS would reject every call with 401.
	 */
	public abstract static class AbstractCallGrantManagementEndpoint extends AbstractCallProtectedResourceWithBearerToken {

		@Override
		protected boolean treatAllHttpStatusAsSuccess() {
			return true;
		}

		@Override
		protected String getUri(Environment env) {
			// presence is guaranteed by @PreEnvironment(strings = GRANT_MANAGEMENT_URL_KEY) on evaluate()
			return env.getString(GRANT_MANAGEMENT_URL_KEY);
		}

		@Override
		protected HttpHeaders getHeaders(Environment env) {
			HttpHeaders headers = super.getHeaders(env);

			JsonObject requestHeaders = env.getObject("resource_endpoint_request_headers");
			if (requestHeaders != null) {
				headers = headersFromJson(requestHeaders, headers);
			}

			return headers;
		}

		protected abstract String getDescription();

		@Override
		protected Environment handleClientResponse(Environment env, JsonObject responseCode, String responseBody, JsonObject responseHeaders, JsonObject fullResponse) {
			env.putObject(GRANT_MANAGEMENT_RESPONSE_KEY, fullResponse);

			// checked whatever the status code was, so the violation is attributed to the response that carried it
			DpopNonceResponseHeader nonceHeader = DpopNonceResponseHeader.from(responseHeaders);
			if (nonceHeader.violation() != null) {
				throw error(nonceHeader.violation(), args("headers", responseHeaders));
			}

			int status = OIDFJSON.getInt(responseCode.get("code"));

			if (status >= 200 && status < 300 && nonceHeader.nonce() != null) {
				// RFC9449 section 8.2 rotation: a nonce on a successful response replaces the stored one for
				// the next request. Nothing went wrong here, so the retry trigger stays unset.
				env.putString("resource_server_dpop_nonce", nonceHeader.nonce());
			}

			if (status == 401 && WwwAuthenticateHeaderValueParser.hasUseDpopNonceChallenge(responseHeaders)) {
				if (nonceHeader.nonce() == null) {
					throw error("The grant management endpoint returned a 'use_dpop_nonce' error but supplied"
						+ " no DPoP-Nonce header, leaving no nonce to retry the request with.",
						args("headers", responseHeaders));
				}
				env.putString("resource_server_dpop_nonce", nonceHeader.nonce());
				// recorded so the caller repeats the call with a proof carrying the nonce
				env.putString(GRANT_MANAGEMENT_DPOP_NONCE_ERROR_KEY, nonceHeader.nonce());
			}

			logSuccess("Got a response from the grant management endpoint (" + getDescription() + "), "
				+ DpopNonceResponseHeader.describeSuppliedNonce(nonceHeader.nonce()), fullResponse);
			return env;
		}
	}

	/**
	 * Calls the grant management endpoint with HTTP GET (query).
	 * Stores the full response as grant_management_response_full.
	 */
	public static class CallGrantManagementEndpointQuery extends AbstractCallGrantManagementEndpoint {

		/**
		 * The body is parsed here, as that is what produces body_json for the conditions that check the
		 * contents of the response; those checks remain separate so their severity stays with the caller.
		 */
		@Override
		protected boolean requireJsonResponseBody() {
			return true;
		}

		/**
		 * A missing or non-JSON body must not fail this condition: the negative tests expect an error
		 * response that need not be JSON at all, and under DPoP the first call may return a bodyless 401
		 * carrying a use_dpop_nonce challenge that {@link #handleClientResponse} has to see in order for
		 * the caller to retry with the nonce. EnsureGrantManagementQueryResponseBodyIsJsonObject reports a
		 * body that should have been JSON and was not.
		 */
		@Override
		protected boolean allowJsonParseFailure() {
			return true;
		}

		@Override
		protected HttpMethod getMethod(Environment env) {
			return HttpMethod.GET;
		}

		@Override
		protected String getDescription() {
			return "query";
		}

		@Override
		@PreEnvironment(required = "access_token", strings = GRANT_MANAGEMENT_URL_KEY)
		@PostEnvironment(required = GRANT_MANAGEMENT_RESPONSE_KEY)
		public Environment evaluate(Environment env) {
			env.removeNativeValue(GRANT_MANAGEMENT_DPOP_NONCE_ERROR_KEY);
			return callProtectedResource(env);
		}
	}

	/**
	 * Calls the grant management endpoint with HTTP DELETE (revoke).
	 * Stores the full response as grant_management_response_full.
	 */
	public static class CallGrantManagementEndpointRevoke extends AbstractCallGrantManagementEndpoint {

		@Override
		protected HttpMethod getMethod(Environment env) {
			return HttpMethod.DELETE;
		}

		@Override
		protected String getDescription() {
			return "revoke";
		}

		@Override
		@PreEnvironment(required = "access_token", strings = GRANT_MANAGEMENT_URL_KEY)
		@PostEnvironment(required = GRANT_MANAGEMENT_RESPONSE_KEY)
		public Environment evaluate(Environment env) {
			env.removeNativeValue(GRANT_MANAGEMENT_DPOP_NONCE_ERROR_KEY);
			return callProtectedResource(env);
		}
	}

	/**
	 * Sets htm/htu on the DPoP proof claims for a call to the grant management endpoint. The htu is the
	 * grant resource URL (GM 6.3), not the configured protected resource URL, so
	 * {@link net.openid.conformance.condition.client.SetDpopHtmHtuForResourceEndpoint} cannot be reused.
	 */
	public abstract static class AbstractSetDpopHtmHtuForGrantManagementEndpoint extends AbstractCondition {

		protected abstract String getHtm();

		@Override
		@PreEnvironment(required = "dpop_proof_claims", strings = GRANT_MANAGEMENT_URL_KEY)
		public Environment evaluate(Environment env) {
			JsonObject claims = env.getObject("dpop_proof_claims");

			claims.addProperty("htm", getHtm());
			claims.addProperty("htu", env.getString(GRANT_MANAGEMENT_URL_KEY));

			logSuccess("Added htm/htu for the grant management endpoint to DPoP proof claims", claims);
			return env;
		}
	}

	public static class SetDpopHtmHtuForGrantManagementQuery extends AbstractSetDpopHtmHtuForGrantManagementEndpoint {
		@Override
		protected String getHtm() {
			return "GET";
		}
	}

	public static class SetDpopHtmHtuForGrantManagementRevoke extends AbstractSetDpopHtmHtuForGrantManagementEndpoint {
		@Override
		protected String getHtm() {
			return "DELETE";
		}
	}

	/**
	 * Builds a DPoP proof for a call to the grant management endpoint and adds it to
	 * resource_endpoint_request_headers. Mirrors CreateDpopProofSteps.createResourceEndpointDpopSteps(),
	 * which cannot be reused because it derives htu from protected_resource_url.
	 */
	public abstract static class AbstractCreateGrantManagementDpopProofSteps extends AbstractConditionSequence {

		protected abstract Class<? extends AbstractSetDpopHtmHtuForGrantManagementEndpoint> htmHtuCondition();

		@Override
		public void evaluate() {
			callAndStopOnFailure(CreateDpopHeader.class);
			callAndStopOnFailure(CreateDpopClaims.class);
			callAndStopOnFailure(htmHtuCondition());
			callAndStopOnFailure(SetDpopAccessTokenHash.class);
			callAndContinueOnFailure(SetDpopProofNonceForResourceEndpoint.class, Condition.ConditionResult.INFO);
			callAndContinueOnFailure(EnsureDpopNonceContainsAllowedCharactersOnly.class, Condition.ConditionResult.FAILURE, "DPOP-8.1");
			callAndStopOnFailure(SignDpopProof.class);
			callAndStopOnFailure(AddDpopHeaderForResourceEndpointRequest.class);
		}
	}

	public static class CreateGrantManagementQueryDpopProofSteps extends AbstractCreateGrantManagementDpopProofSteps {
		@Override
		protected Class<? extends AbstractSetDpopHtmHtuForGrantManagementEndpoint> htmHtuCondition() {
			return SetDpopHtmHtuForGrantManagementQuery.class;
		}
	}

	public static class CreateGrantManagementRevokeDpopProofSteps extends AbstractCreateGrantManagementDpopProofSteps {
		@Override
		protected Class<? extends AbstractSetDpopHtmHtuForGrantManagementEndpoint> htmHtuCondition() {
			return SetDpopHtmHtuForGrantManagementRevoke.class;
		}
	}

	/**
	 * Verifies that the grant management endpoint returned HTTP 200.
	 *
	 * <p>A 503 never reaches here: GM 6.4 permits it outright, so the caller skips the test rather than
	 * reporting a permitted response as a conformance failure.
	 */
	public static class EnsureGrantManagementQuerySucceeded extends AbstractCondition {

		@Override
		@PreEnvironment(required = GRANT_MANAGEMENT_RESPONSE_KEY)
		public Environment evaluate(Environment env) {
			int code = env.getInteger(GRANT_MANAGEMENT_RESPONSE_KEY, "status");
			if (code != 200) {
				throw error("Expected HTTP 200 from grant management query endpoint",
					args("http_status", code,
						"body", env.getString(GRANT_MANAGEMENT_RESPONSE_KEY, "body")));
			}
			logSuccess("Grant management query endpoint returned HTTP 200");
			return env;
		}
	}

	/**
	 * Reports that the authorization server was still demanding a new DPoP nonce after the conformance
	 * suite had already retried the grant management call with the nonce the server supplied.
	 *
	 * <p>Without this the retry loop simply gives up and the tester is left looking at "Expected HTTP 200"
	 * with no indication that the cause was an endless use_dpop_nonce loop.
	 */
	public static class EnsureGrantManagementDpopNonceRetryWasAccepted extends AbstractCondition {

		@Override
		@PreEnvironment(required = GRANT_MANAGEMENT_RESPONSE_KEY)
		public Environment evaluate(Environment env) {
			throw error("The authorization server answered the grant management call with a 'use_dpop_nonce'"
					+ " error again, after the request had already been retried with the nonce it supplied."
					+ " RFC9449 section 8.2 expects a proof carrying the supplied nonce to be accepted, so the"
					+ " conformance suite gave up rather than retrying indefinitely.",
				args("http_status", env.getInteger(GRANT_MANAGEMENT_RESPONSE_KEY, "status"),
					"www-authenticate", env.getString(GRANT_MANAGEMENT_RESPONSE_KEY, "headers.www-authenticate")));
		}
	}

	/**
	 * Verifies the grant management query response has Content-Type: application/json.
	 * GM 6.4 says the AS "will respond with a JSON-formatted response".
	 */
	public static class EnsureGrantManagementQueryResponseContentTypeIsJson extends AbstractCheckEndpointContentTypeReturned {

		@Override
		@PreEnvironment(required = GRANT_MANAGEMENT_RESPONSE_KEY)
		public Environment evaluate(Environment env) {
			return checkContentType(env, GRANT_MANAGEMENT_RESPONSE_KEY, "headers.", "application/json");
		}
	}

	/**
	 * Verifies the grant management query response is marked as non-cacheable.
	 * GM 6.4 only shows "Cache-Control: no-cache, no-store" in its (non-normative) examples, so
	 * callers should invoke this with WARNING severity.
	 */
	public static class EnsureGrantManagementQueryResponseIsNotCacheable extends AbstractCondition {

		@Override
		@PreEnvironment(required = GRANT_MANAGEMENT_RESPONSE_KEY)
		public Environment evaluate(Environment env) {
			String cacheControl = env.getString(GRANT_MANAGEMENT_RESPONSE_KEY, "headers.cache-control");
			if (cacheControl == null || cacheControl.isEmpty()) {
				throw error("Cache-Control header missing from grant management query response; the examples in GM 6.4 use 'no-cache, no-store' as the grant contents are sensitive",
					args("headers", env.getElementFromObject(GRANT_MANAGEMENT_RESPONSE_KEY, "headers")));
			}
			if (!cacheControl.toLowerCase(Locale.ROOT).contains("no-store")) {
				throw error("Cache-Control header on the grant management query response does not contain 'no-store'",
					args("cache-control", cacheControl));
			}
			logSuccess("Grant management query response is marked as non-cacheable", args("cache-control", cacheControl));
			return env;
		}
	}

	/** Timestamps before this are not plausible - it predates the specifications involved. */
	private static final long EARLIEST_PLAUSIBLE_TIMESTAMP = 1325376000L; // 2012-01-01T00:00:00Z

	/** 50 years, used as the upper bound on how far into the future a timestamp may plausibly be. */
	private static final long MAX_FUTURE_SECONDS = 50L * 365 * 24 * 60 * 60;

	/** Allowance for clock differences between the AS and the conformance suite. */
	private static final long CLOCK_SKEW_SECONDS = 5 * 60;

	/**
	 * Base for the conditions that inspect the body of a grant management query response.
	 */
	public abstract static class AbstractGrantManagementQueryResponseCondition extends AbstractCondition {

		protected JsonObject queryResponseBody(Environment env) {
			JsonElement bodyElement = env.getElementFromObject(GRANT_MANAGEMENT_RESPONSE_KEY, "body_json");

			if (bodyElement == null || !bodyElement.isJsonObject()) {
				throw error("Grant management query response body is missing or not a JSON object",
					args(GRANT_MANAGEMENT_RESPONSE_KEY, env.getObject(GRANT_MANAGEMENT_RESPONSE_KEY)));
			}

			return bodyElement.getAsJsonObject();
		}
	}

	/**
	 * Checks that the body of a successful grant management query response is a JSON object at all.
	 * Split out from the content checks so those can be run - and reported - independently.
	 */
	public static class EnsureGrantManagementQueryResponseBodyIsJsonObject extends AbstractGrantManagementQueryResponseCondition {

		@Override
		@PreEnvironment(required = GRANT_MANAGEMENT_RESPONSE_KEY)
		public Environment evaluate(Environment env) {
			JsonObject body = queryResponseBody(env);

			logSuccess("Grant management query response body is a JSON object", args("body", body));
			return env;
		}
	}

	/**
	 * Checks that the queried grant actually describes some permissions, i.e. that at least one of scopes,
	 * claims or authorization_details is present.
	 *
	 * GM 6.4 makes every member of the response OPTIONAL, so this cannot be a failure: a response that
	 * describes none of the granted permissions is useless to the client but is not a spec violation.
	 * Callers therefore invoke this with WARNING severity.
	 */
	public static class CheckGrantManagementQueryResponseContainsGrantDetails extends AbstractGrantManagementQueryResponseCondition {

		@Override
		@PreEnvironment(required = GRANT_MANAGEMENT_RESPONSE_KEY)
		public Environment evaluate(Environment env) {
			JsonObject body = queryResponseBody(env);

			if (!body.has("scopes") && !body.has("claims") && !body.has("authorization_details")) {
				throw error("Grant management query response contains none of scopes, claims or authorization_details, so it does not tell the client what was granted. All members are OPTIONAL per GM 6.4, so this is a warning rather than a failure",
					args("body", body));
			}

			logSuccess("Grant management query response describes the granted permissions", args("body", body));
			return env;
		}
	}

	/**
	 * Checks that the grant the AS reports actually covers what it just granted at the token endpoint.
	 *
	 * <p>GM 6.4 says the query response describes "the grant", so every scope the token endpoint reported
	 * as granted would be expected to appear in it. This is what distinguishes a real merge or replace from
	 * an AS that accepted the action and left the grant as it was. All members of the response are OPTIONAL,
	 * so a response with no 'scopes' member is skipped rather than failed - it is already reported by
	 * {@link CheckGrantManagementQueryResponseContainsGrantDetails}.
	 *
	 * <p>The scopes GM 6.1 defines for the grant management API itself ({@link #GRANT_MANAGEMENT_API_SCOPES})
	 * are excluded from the comparison: they authorize calls to the grant management endpoint rather than
	 * describing permissions held in the grant, so an AS that echoes them in the granted scope but omits
	 * them from the grant is behaving correctly.
	 *
	 * <p>Nothing in GM states normatively that the query response must enumerate the granted scope - 6.4
	 * makes every member OPTIONAL - so this is an interpretation of "describes the grant" and callers
	 * invoke it with WARNING severity.
	 */
	public static class CheckGrantManagementQueryResponseCoversGrantedScope extends AbstractGrantManagementQueryResponseCondition {

		@Override
		@PreEnvironment(required = { GRANT_MANAGEMENT_RESPONSE_KEY, "client" })
		public Environment evaluate(Environment env) {
			JsonObject body = queryResponseBody(env);

			String grantedScope = env.getString("client", ExtractGrantedScopeFromTokenEndpointResponse.GRANTED_SCOPE);
			if (Strings.isNullOrEmpty(grantedScope)) {
				logSuccess("The token endpoint response did not report a granted scope, so there is nothing to compare the grant against");
				return env;
			}

			JsonElement scopesElement = body.get("scopes");
			if (scopesElement == null || !scopesElement.isJsonArray()) {
				logSuccess("The grant management query response has no 'scopes' member, which GM 6.4 permits, so the granted scope cannot be compared",
					args("body", body));
				return env;
			}

			Set<String> scopesInGrant = new LinkedHashSet<>();
			for (JsonElement entry : scopesElement.getAsJsonArray()) {
				if (entry.isJsonObject() && entry.getAsJsonObject().has("scope")) {
					scopesInGrant.addAll(Arrays.asList(
						OIDFJSON.getString(entry.getAsJsonObject().get("scope")).split(" ")));
				}
			}

			List<String> missing = Arrays.stream(grantedScope.split(" "))
				.filter(scope -> !scope.isEmpty())
				.filter(scope -> !GRANT_MANAGEMENT_API_SCOPES.contains(scope))
				.filter(scope -> !scopesInGrant.contains(scope))
				.toList();

			if (!missing.isEmpty()) {
				throw error("The grant management query response does not cover every scope the token endpoint"
						+ " reported as granted, so the grant does not appear to describe the access that was just"
						+ " issued. Note that this is the conformance suite's interpretation of GM 6.4 'describes"
						+ " the grant' - GM 6.4 makes every member of the response OPTIONAL and does not state that"
						+ " the granted scope must be enumerated, so this is reported as a warning.",
					args("missing_scopes", missing, "granted_scope", grantedScope, "scopes_in_grant", scopesInGrant,
						"excluded_grant_management_api_scopes", new TreeSet<>(GRANT_MANAGEMENT_API_SCOPES)));
			}

			logSuccess("The grant covers every scope the token endpoint reported as granted",
				args("granted_scope", grantedScope, "scopes_in_grant", scopesInGrant));
			return env;
		}
	}

	/**
	 * Validates the grant management query response against the structure GM 6.4 defines: the shape of
	 * scopes/claims/authorization_details, the types of the timestamps and the values of updated_by.
	 */
	public static class ValidateGrantManagementQueryResponseSchema extends AbstractJsonSchemaBasedValidation {

		@Override
		protected JsonSchemaValidationInput createJsonSchemaValidationInput(Environment env) {
			return grantManagementQueryResponseSchemaInput(env);
		}

		@Override
		@PreEnvironment(required = GRANT_MANAGEMENT_RESPONSE_KEY)
		public Environment evaluate(Environment env) {
			return super.evaluate(env);
		}

		/**
		 * Members the schema does not define are reported by
		 * {@link CheckForUnexpectedParametersInGrantManagementQueryResponse} instead, so the caller can
		 * treat them as a warning while a structural error stays a failure.
		 */
		@Override
		protected void onValidationFailure(Environment env, JsonSchemaValidationResult validationResult, JsonSchemaValidationInput input) {
			JsonSchemaValidationResult structuralErrors = validationResult.withoutUnknownPropertyErrors();
			if (!structuralErrors.isValid()) {
				super.onValidationFailure(env, structuralErrors, input);
			}
		}
	}

	/**
	 * Warns about members of the grant management query response that GM 6.4 does not define, at any depth
	 * - usually typos, e.g. 'scope' instead of 'scopes'. Callers invoke this with WARNING severity, as the
	 * specification does not forbid additional members.
	 */
	public static class CheckForUnexpectedParametersInGrantManagementQueryResponse extends AbstractCheckForUnexpectedSchemaProperties {

		@Override
		protected JsonSchemaValidationInput createJsonSchemaValidationInput(Environment env) {
			return grantManagementQueryResponseSchemaInput(env);
		}

		@Override
		@PreEnvironment(required = GRANT_MANAGEMENT_RESPONSE_KEY)
		public Environment evaluate(Environment env) {
			return super.evaluate(env);
		}
	}

	private static JsonSchemaValidationInput grantManagementQueryResponseSchemaInput(Environment env) {
		JsonElement body = env.getElementFromObject(GRANT_MANAGEMENT_RESPONSE_KEY, "body_json");
		return new JsonSchemaValidationInput("Grant management query response",
			"json-schemas/grant-management/query_response.json",
			body != null && body.isJsonObject() ? body.getAsJsonObject() : null);
	}

	/**
	 * Checks what a JSON schema cannot: that the timestamps in the query response make sense relative to
	 * each other and to now. A value in the wrong direction (a grant created in the future, or one that
	 * expired before it was successfully queried) indicates a broken implementation just as much as a
	 * malformed one does, so both bounds are checked on each timestamp.
	 */
	public static class ValidateGrantManagementQueryResponseTimestamps extends AbstractGrantManagementQueryResponseCondition {

		/** The name GM 6.4's examples give the last-updated timestamp. */
		private static final String LAST_UPDATED_AT_MEMBER = "last_updated_at";

		/** The name GM 6.4's prose gives the same timestamp. */
		private static final String LAST_UPDATED_MEMBER = "last_updated";

		@Override
		@PreEnvironment(required = GRANT_MANAGEMENT_RESPONSE_KEY)
		public Environment evaluate(Environment env) {
			JsonObject body = queryResponseBody(env);
			long now = Instant.now().getEpochSecond();

			Long createdAt = validateTimestamp(body, "created_at", now);
			Long expiresAt = validateTimestamp(body, "expires_at", now);

			// GM 6.4 is internally inconsistent: its prose calls this member 'last_updated' while the
			// examples in the same section use 'last_updated_at'. Accept whichever the AS sent.
			String lastUpdatedMember = body.has(LAST_UPDATED_AT_MEMBER) ? LAST_UPDATED_AT_MEMBER
				: (body.has(LAST_UPDATED_MEMBER) ? LAST_UPDATED_MEMBER : null);
			Long lastUpdated = lastUpdatedMember == null ? null : validateTimestamp(body, lastUpdatedMember, now);

			if (createdAt != null && createdAt > now + CLOCK_SKEW_SECONDS) {
				throw error("'created_at' in the grant management query response is in the future",
					args("created_at", createdAt, "now", now));
			}

			if (lastUpdated != null && lastUpdated > now + CLOCK_SKEW_SECONDS) {
				throw error("'" + lastUpdatedMember + "' in the grant management query response is in the future",
					args(lastUpdatedMember, lastUpdated, "now", now));
			}

			if (createdAt != null && lastUpdated != null && lastUpdated < createdAt) {
				throw error("'" + lastUpdatedMember + "' in the grant management query response is before 'created_at'",
					args("created_at", createdAt, lastUpdatedMember, lastUpdated));
			}

			if (expiresAt != null && expiresAt < now - CLOCK_SKEW_SECONDS) {
				throw error("'expires_at' in the grant management query response is in the past, but the grant was successfully queried",
					args("expires_at", expiresAt, "now", now));
			}

			if (body.has(LAST_UPDATED_AT_MEMBER) && body.has(LAST_UPDATED_MEMBER)) {
				throw error("The grant management query response contains both '" + LAST_UPDATED_AT_MEMBER
					+ "' and '" + LAST_UPDATED_MEMBER + "'; GM 6.4 describes a single member (its prose and"
					+ " its examples disagree on the name), so only one of the two should be returned",
					args("body", body));
			}

			logSuccess("The timestamps in the grant management query response are consistent"
					+ (lastUpdatedMember == null ? "" : ", using '" + lastUpdatedMember + "' as the last-updated time"),
				args("body", body));
			return env;
		}

		private Long validateTimestamp(JsonObject body, String member, long now) {
			if (!body.has(member)) {
				return null;
			}

			long value;
			try {
				value = OIDFJSON.getLong(body.get(member));
			} catch (OIDFJSON.UnexpectedJsonTypeException e) {
				throw error("'" + member + "' in the grant management query response is not a number; GM 6.4 defines it as a NumericDate",
					args(member, body.get(member)));
			}

			if (value < EARLIEST_PLAUSIBLE_TIMESTAMP) {
				throw error("'" + member + "' in the grant management query response is implausibly far in the past; it should be seconds since the epoch",
					args(member, value));
			}

			if (value > now + MAX_FUTURE_SECONDS) {
				throw error("'" + member + "' in the grant management query response is implausibly far in the future; it should be seconds since the epoch",
					args(member, value, "now", now));
			}

			return value;
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
	 * Verifies that the revoke response carried no body, as GM 6.5 requires alongside the 204.
	 *
	 * <p>Split out from {@link EnsureGrantManagementRevokeSucceeded} so the status and the body are
	 * reported independently.
	 */
	public static class EnsureGrantManagementRevokeResponseBodyIsEmpty extends AbstractCondition {

		@Override
		@PreEnvironment(required = GRANT_MANAGEMENT_RESPONSE_KEY)
		public Environment evaluate(Environment env) {
			// a 204 response has no body, which the HTTP machinery records as JsonNull -
			// env.getString() would reject that rather than return null
			JsonElement bodyElement = env.getElementFromObject(GRANT_MANAGEMENT_RESPONSE_KEY, "body");
			String body = bodyElement == null || bodyElement.isJsonNull() ? null : OIDFJSON.getString(bodyElement);

			if (!Strings.isNullOrEmpty(body)) {
				throw error("The grant management revoke response contained a body. GM 6.5 requires the"
						+ " authorization server to respond with 204 and an empty response body.",
					args("body", body));
			}

			logSuccess("The grant management revoke response body is empty, as GM 6.5 requires");
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
	 * Verifies that the grant management endpoint returned HTTP 403 or 404 when a client that does not own
	 * the grant used it.
	 *
	 * GM 6.6 gives 403 for a client that is not authorized to access the grant and 404 for a grant resource
	 * URL that is unknown. Accepting either is an interpretation: an AS may legitimately answer 404 to hide
	 * the existence of another client's grant.
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

	/** The actions GM 5.2 defines for the grant_management_action authorization request parameter. */
	private static final Set<String> GRANT_MANAGEMENT_ACTIONS = Set.of("create", "merge", "replace");

	/**
	 * Base for the checks on the grant management parameters an RP under test sent to the emulated PAR
	 * endpoint. The parameters may arrive either as PAR form parameters or as claims of a signed request
	 * object, so both are consulted; the request object wins, matching how an AS would process the request.
	 */
	public abstract static class AbstractCheckGrantManagementParametersInPARRequest extends AbstractCondition {

		protected String getGrantManagementParameter(Environment env, String name) {
			JsonElement fromRequestObject = env.getElementFromObject("authorization_request_object", "claims." + name);
			if (fromRequestObject != null && fromRequestObject.isJsonPrimitive()) {
				return OIDFJSON.getString(fromRequestObject);
			}

			JsonElement fromFormParams = env.getElementFromObject("par_endpoint_http_request_params", name);
			if (fromFormParams != null && fromFormParams.isJsonPrimitive()) {
				return OIDFJSON.getString(fromFormParams);
			}

			return null;
		}

		protected JsonObject requestParametersForLogging(Environment env) {
			JsonObject args = new JsonObject();
			JsonElement requestObjectClaims = env.getElementFromObject("authorization_request_object", "claims");
			if (requestObjectClaims != null) {
				args.add("request_object_claims", requestObjectClaims);
			}
			JsonObject formParams = env.getObject("par_endpoint_http_request_params");
			if (formParams != null) {
				args.add("par_endpoint_request_parameters", formParams);
			}
			return args;
		}
	}

	/**
	 * Checks that the RP sent a grant_management_action the specification defines. Without this the grant
	 * management client tests would pass for a client that made an ordinary authorization request.
	 */
	public static class EnsurePARRequestContainsGrantManagementAction extends AbstractCheckGrantManagementParametersInPARRequest {

		@Override
		@PreEnvironment(required = "par_endpoint_http_request_params")
		public Environment evaluate(Environment env) {
			String action = getGrantManagementParameter(env, "grant_management_action");

			if (action == null) {
				throw error("The pushed authorization request does not contain a grant_management_action parameter. Configure your client to use grant management, as described in the test description",
					requestParametersForLogging(env));
			}

			if (!GRANT_MANAGEMENT_ACTIONS.contains(action)) {
				throw error("The pushed authorization request contains a grant_management_action that GM 5.2 does not define",
					args("grant_management_action", action, "expected", GRANT_MANAGEMENT_ACTIONS));
			}

			logSuccess("The pushed authorization request contains a valid grant_management_action",
				args("grant_management_action", action));
			return env;
		}
	}

	/**
	 * Checks that the RP sent an action that updates an existing grant, i.e. merge or replace (GM 5.2).
	 *
	 * <p>Used by the tests that ask the client to update a grant. The generic
	 * {@link EnsurePARRequestContainsGrantManagementAction} accepts any of the three actions, so on its own
	 * it would let 'create' plus a grant_id through - a combination GM 5.4 says should yield
	 * invalid_request, not invalid_grant_id, so the test would not be exercising what it claims to.
	 */
	public static class EnsurePARRequestContainsGrantManagementActionUpdatingAGrant
			extends AbstractCheckGrantManagementParametersInPARRequest {

		private static final Set<String> UPDATING_ACTIONS = Set.of("merge", "replace");

		@Override
		@PreEnvironment(required = "par_endpoint_http_request_params")
		public Environment evaluate(Environment env) {
			String action = getGrantManagementParameter(env, "grant_management_action");

			if (!UPDATING_ACTIONS.contains(action)) {
				throw error("This test requires the client to update an existing grant, so GM 5.2 requires"
						+ " grant_management_action to be 'merge' or 'replace'. Configure your client as"
						+ " described in the test description.",
					args("grant_management_action", action, "expected", new TreeSet<>(UPDATING_ACTIONS)));
			}

			logSuccess("The pushed authorization request updates an existing grant",
				args("grant_management_action", action));
			return env;
		}
	}

	/**
	 * Checks that the RP sent a grant_id, for the tests where the client is expected to be updating an
	 * existing grant (GM 5.2: grant_id is used together with the merge and replace actions).
	 */
	public static class EnsurePARRequestContainsGrantId extends AbstractCheckGrantManagementParametersInPARRequest {

		@Override
		@PreEnvironment(required = "par_endpoint_http_request_params")
		public Environment evaluate(Environment env) {
			String grantId = getGrantManagementParameter(env, "grant_id");

			if (grantId == null || grantId.isEmpty()) {
				throw error("The pushed authorization request does not contain a grant_id parameter. Configure your client to update an existing grant, as described in the test description",
					requestParametersForLogging(env));
			}

			logSuccess("The pushed authorization request contains a grant_id", args("grant_id", grantId));
			return env;
		}
	}

	/**
	 * Checks GM 5.2's requirement that grant_id "MUST NOT" be present when the action is create.
	 */
	public static class EnsurePARRequestDoesNotContainGrantIdWithCreateAction extends AbstractCheckGrantManagementParametersInPARRequest {

		@Override
		@PreEnvironment(required = "par_endpoint_http_request_params")
		public Environment evaluate(Environment env) {
			String action = getGrantManagementParameter(env, "grant_management_action");
			String grantId = getGrantManagementParameter(env, "grant_id");

			if ("create".equals(action) && grantId != null) {
				throw error("The pushed authorization request contains a grant_id alongside grant_management_action=create; GM 5.4 requires the authorization server to respond with invalid_request when grant_id is provided for the create action",
					args("grant_management_action", action, "grant_id", grantId));
			}

			logSuccess("The pushed authorization request does not contain a grant_id with the create action",
				args("grant_management_action", action));
			return env;
		}
	}

	/**
	 * Advertises grant management support in the discovery document the suite serves as the emulated AS
	 * (GM 7.1), so that a spec-following client has a reason to send grant_management_action at all.
	 *
	 * The advertised endpoint is served by AbstractFAPI2SPFinalClientTestGrantManagement, which emulates
	 * the query and revoke calls of GM 6 so an RP can be tested against the whole grant lifecycle.
	 */
	public static class AddGrantManagementToServerConfiguration extends AbstractCondition {

		@Override
		@PreEnvironment(required = "server")
		@PostEnvironment(required = "server")
		public Environment evaluate(Environment env) {
			JsonObject server = env.getObject("server");

			if (!server.has("token_endpoint")) {
				throw error("The server configuration has no token_endpoint yet; the grant management endpoint is derived from it, so this condition must run after the endpoints have been generated",
					args("server", server));
			}

			String tokenEndpoint = OIDFJSON.getString(server.get("token_endpoint"));
			String grantManagementEndpoint = deriveGrantManagementEndpoint(tokenEndpoint, "token_endpoint");

			if (server.has("mtls_endpoint_aliases")) {
				JsonElement mtlsAliasesElement = server.get("mtls_endpoint_aliases");
				if (!mtlsAliasesElement.isJsonObject()) {
					throw error("The mtls_endpoint_aliases in the server configuration is not a JSON object",
						args("mtls_endpoint_aliases", mtlsAliasesElement));
				}
				JsonObject mtlsAliases = mtlsAliasesElement.getAsJsonObject();
				if (!mtlsAliases.has("token_endpoint")) {
					throw error("The mtls_endpoint_aliases in the server configuration have no token_endpoint; the mTLS grant management endpoint is derived from it",
						args("mtls_endpoint_aliases", mtlsAliases));
				}
				String mtlsTokenEndpoint = OIDFJSON.getString(mtlsAliases.get("token_endpoint"));
				mtlsAliases.addProperty("grant_management_endpoint",
					deriveGrantManagementEndpoint(mtlsTokenEndpoint, "mtls_endpoint_aliases.token_endpoint"));
			}

			server.addProperty("grant_management_endpoint", grantManagementEndpoint);

			JsonArray actions = new JsonArray();
			for (String action : new String[] { "create", "merge", "replace", "query", "revoke" }) {
				actions.add(action);
			}
			server.add("grant_management_actions_supported", actions);

			logSuccess("Added grant management metadata to the server configuration",
				args("grant_management_endpoint", grantManagementEndpoint,
					"grant_management_actions_supported", actions));

			return env;
		}

		/**
		 * The emulated OP's grant management endpoint sits alongside its token endpoint. A path that does
		 * not end in "token" would silently produce a grant management endpoint identical to the token
		 * endpoint, so that is rejected rather than left to fail later in a confusing way.
		 */
		private String deriveGrantManagementEndpoint(String tokenEndpoint, String member) {
			if (!tokenEndpoint.endsWith("token")) {
				throw error("The " + member + " in the server configuration does not end in 'token', so the"
						+ " grant management endpoint cannot be derived from it",
					args(member, tokenEndpoint));
			}
			return tokenEndpoint.substring(0, tokenEndpoint.length() - "token".length()) + "grants";
		}
	}

	/**
	 * Checks that the grant resource URL the RP called identifies the grant the suite issued (GM 6.3:
	 * [grant_management_endpoint]/[grant_id]). A client asking about some other grant has either
	 * fabricated an identifier or lost track of the one it was given.
	 */
	public static class EnsureGrantManagementRequestIsForIssuedGrant extends AbstractCondition {

		public static final String REQUESTED_GRANT_ID = "incoming_grant_management_grant_id";

		@Override
		@PreEnvironment(strings = { REQUESTED_GRANT_ID, GRANT_ID_KEY })
		public Environment evaluate(Environment env) {
			String requested = env.getString(REQUESTED_GRANT_ID);
			String issued = env.getString(GRANT_ID_KEY);

			if (!issued.equals(requested)) {
				throw error("The client called the grant management endpoint for a grant this test never issued",
					args("requested_grant_id", requested, "issued_grant_id", issued));
			}

			logSuccess("The client called the grant management endpoint for the grant it was issued",
				args("grant_id", requested));
			return env;
		}
	}

	/**
	 * Builds the response to an RP's query of a grant (GM 6.4): the granted scopes, when the grant was
	 * created, and the Cache-Control header the examples in that section use, as the contents are sensitive.
	 */
	public static class CreateGrantManagementQueryResponse extends AbstractCondition {

		public static final String RESPONSE_KEY = "grant_management_endpoint_response";

		public static final String RESPONSE_HEADERS_KEY = "grant_management_endpoint_response_headers";

		@Override
		@PreEnvironment(required = "client")
		@PostEnvironment(required = { RESPONSE_KEY, RESPONSE_HEADERS_KEY })
		public Environment evaluate(Environment env) {
			JsonObject grant = new JsonObject();

			String scope = env.getString("client", "scope");
			if (scope != null && !scope.isEmpty()) {
				JsonArray scopes = new JsonArray();
				JsonObject scopeEntry = new JsonObject();
				scopeEntry.addProperty("scope", scope);
				scopes.add(scopeEntry);
				grant.add("scopes", scopes);
			}

			grant.addProperty("created_at", Instant.now().getEpochSecond());
			grant.addProperty("updated_by", "client");

			env.putObject(RESPONSE_KEY, grant);

			JsonObject headers = new JsonObject();
			headers.addProperty("Cache-Control", "no-cache, no-store");
			env.putObject(RESPONSE_HEADERS_KEY, headers);

			logSuccess("Created the grant management query response", grant);
			return env;
		}
	}

	/**
	 * Creates an invalid_grant_id error response for the PAR endpoint.
	 * Sets par_endpoint_response, par_endpoint_response_headers, and par_endpoint_response_http_status.
	 * Used in client (RP) tests to simulate the AS rejecting an invalid grant_id at the PAR endpoint.
	 */
	public static class CreatePAREndpointInvalidGrantIdErrorResponse extends AbstractCondition {

		@Override
		@PostEnvironment(required = {"par_endpoint_response", "par_endpoint_response_headers"}, integers = "par_endpoint_response_http_status")
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

}
