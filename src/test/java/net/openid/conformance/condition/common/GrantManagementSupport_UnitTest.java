package net.openid.conformance.condition.common;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static net.openid.conformance.condition.common.GrantManagementSupport.GRANT_ID_KEY;
import static net.openid.conformance.condition.common.GrantManagementSupport.GRANT_MANAGEMENT_RESPONSE_KEY;
import static net.openid.conformance.condition.common.GrantManagementSupport.GRANT_MANAGEMENT_URL_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class GrantManagementSupport_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private <T extends AbstractCondition> T cond(T condition) {
		condition.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		return condition;
	}

	private static JsonObject json(String s) {
		return JsonParser.parseString(s).getAsJsonObject();
	}

	/**
	 * Builds the response object shape produced by AbstractCallProtectedResourceWithBearerToken:
	 * a status, a header map and (optionally) a parsed body.
	 */
	private static JsonObject response(int status, String headersJson, String bodyJson) {
		JsonObject o = new JsonObject();
		o.addProperty("status", status);
		o.add("headers", json(headersJson));
		if (bodyJson != null) {
			o.add("body_json", json(bodyJson));
		}
		return o;
	}

	// ----  authorization request builders  ----

	@Test
	public void addGrantManagementScopes_appendsToExistingScope() {
		env.putObject("authorization_endpoint_request", json("{\"scope\":\"openid payments\"}"));

		cond(new GrantManagementSupport.AddGrantManagementScopesToAuthorizationRequest()).execute(env);

		assertEquals("openid payments grant_management_query grant_management_revoke",
			env.getString("authorization_endpoint_request", "scope"));
	}

	@Test
	public void addGrantManagementScopes_noExistingScopeDoesNotLeaveLeadingSpace() {
		env.putObject("authorization_endpoint_request", json("{}"));

		cond(new GrantManagementSupport.AddGrantManagementScopesToAuthorizationRequest()).execute(env);

		assertEquals("grant_management_query grant_management_revoke",
			env.getString("authorization_endpoint_request", "scope"));
	}

	@Test
	public void addGrantManagementActions_setSpecActionValues() {
		env.putObject("authorization_endpoint_request", json("{}"));
		cond(new GrantManagementSupport.AddGrantManagementActionCreateToAuthorizationRequest()).execute(env);
		assertEquals("create", env.getString("authorization_endpoint_request", "grant_management_action"));

		cond(new GrantManagementSupport.AddGrantManagementActionMergeToAuthorizationRequest()).execute(env);
		assertEquals("merge", env.getString("authorization_endpoint_request", "grant_management_action"));

		cond(new GrantManagementSupport.AddGrantManagementActionReplaceToAuthorizationRequest()).execute(env);
		assertEquals("replace", env.getString("authorization_endpoint_request", "grant_management_action"));
	}

	@Test
	public void addGrantIdToAuthorizationRequest_copiesGrantIdFromEnvironment() {
		env.putObject("authorization_endpoint_request", json("{}"));
		env.putString(GRANT_ID_KEY, "TSdqirmAxDa0_-DB_1bASQ");

		cond(new GrantManagementSupport.AddGrantIdToAuthorizationRequest()).execute(env);

		assertEquals("TSdqirmAxDa0_-DB_1bASQ", env.getString("authorization_endpoint_request", "grant_id"));
	}

	// ----  ExtractGrantIdFromTokenResponse  ----

	@Test
	public void extractGrantId_acceptsHighEntropyValue() {
		env.putObject("token_endpoint_response", json("{\"grant_id\":\"TSdqirmAxDa0_-DB_1bASQ\"}"));

		cond(new GrantManagementSupport.ExtractGrantIdFromTokenResponse()).execute(env);

		assertEquals("TSdqirmAxDa0_-DB_1bASQ", env.getString(GRANT_ID_KEY));
	}

	@Test
	public void extractGrantId_failsWhenMissing() {
		env.putObject("token_endpoint_response", json("{\"access_token\":\"abc\"}"));

		assertThrows(ConditionError.class,
			() -> cond(new GrantManagementSupport.ExtractGrantIdFromTokenResponse()).execute(env));
	}

	@Test
	public void extractGrantId_failsWhenEmpty() {
		env.putObject("token_endpoint_response", json("{\"grant_id\":\"\"}"));

		assertThrows(ConditionError.class,
			() -> cond(new GrantManagementSupport.ExtractGrantIdFromTokenResponse()).execute(env));
	}

	@Test
	public void extractGrantId_doesNotJudgeTheValueItself() {
		// short values are the entropy check's business, not the extraction's - see
		// checkGrantIdQuality_flagsShortValue below
		env.putObject("token_endpoint_response", json("{\"grant_id\":\"1234567890123456789\"}"));

		cond(new GrantManagementSupport.ExtractGrantIdFromTokenResponse()).execute(env);

		assertEquals("1234567890123456789", env.getString(GRANT_ID_KEY));
	}

	// ----  CheckGrantIdIsUrlSafe / CheckGrantIdHasSufficientEntropy  ----

	@Test
	public void checkGrantIdQuality_acceptsUrlSafeHighEntropyValue() {
		env.putString(GRANT_ID_KEY, "TSdqirmAxDa0_-DB_1bASQ~.");

		cond(new GrantManagementSupport.CheckGrantIdIsUrlSafe()).execute(env);
		cond(new GrantManagementSupport.CheckGrantIdHasSufficientEntropy()).execute(env);
	}

	@Test
	public void checkGrantIdQuality_acceptsExactlyTheSuggestedMinimumLength() {
		env.putString(GRANT_ID_KEY, "12345678901234567890");

		cond(new GrantManagementSupport.CheckGrantIdHasSufficientEntropy()).execute(env);
	}

	@Test
	public void checkGrantIdQuality_flagsShortValue() {
		// 19 characters - one below the length the suite suggests
		env.putString(GRANT_ID_KEY, "1234567890123456789");

		assertThrows(ConditionError.class,
			() -> cond(new GrantManagementSupport.CheckGrantIdHasSufficientEntropy()).execute(env));
	}

	/** A short value is only a warning about entropy, never a URL safety violation. */
	@Test
	public void checkGrantIdIsUrlSafe_ignoresLength() {
		env.putString(GRANT_ID_KEY, "abc");

		cond(new GrantManagementSupport.CheckGrantIdIsUrlSafe()).execute(env);
	}

	@Test
	public void checkGrantIdQuality_flagsCharactersThatAreNotUrlSafe() {
		env.putString(GRANT_ID_KEY, "grant id/with?unsafe=characters");

		assertThrows(ConditionError.class,
			() -> cond(new GrantManagementSupport.CheckGrantIdIsUrlSafe()).execute(env));
	}

	@Test
	public void checkGrantIdIsUrlSafe_acceptsTheRestOfTheRfc3986PathSegmentAlphabet() {
		// GM 5.5 says only "URL safe"; RFC 3986 pchar allows sub-delims, ":" and "@" unencoded
		env.putString(GRANT_ID_KEY, "grant!$&'()*+,;=:@id");

		cond(new GrantManagementSupport.CheckGrantIdIsUrlSafe()).execute(env);
	}

	// ----  SetGrantManagementEndpointUrl  ----

	@Test
	public void setGrantManagementUrl_insertsSeparator() {
		env.putObject("server", json("{\"grant_management_endpoint\":\"https://as.example.com/grants\"}"));
		env.putString(GRANT_ID_KEY, "abc");

		cond(new GrantManagementSupport.SetGrantManagementEndpointUrl()).execute(env);

		assertEquals("https://as.example.com/grants/abc", env.getString(GRANT_MANAGEMENT_URL_KEY));
	}

	@Test
	public void setGrantManagementUrl_doesNotDoubleSeparator() {
		env.putObject("server", json("{\"grant_management_endpoint\":\"https://as.example.com/grants/\"}"));
		env.putString(GRANT_ID_KEY, "abc");

		cond(new GrantManagementSupport.SetGrantManagementEndpointUrl()).execute(env);

		assertEquals("https://as.example.com/grants/abc", env.getString(GRANT_MANAGEMENT_URL_KEY));
	}

	@Test
	public void setGrantManagementUrl_encodesTheGrantIdAsASinglePathSegment() {
		env.putObject("server", json("{\"grant_management_endpoint\":\"https://as.example.com/grants\"}"));
		env.putString(GRANT_ID_KEY, "../../admin?x=1");

		cond(new GrantManagementSupport.SetGrantManagementEndpointUrl()).execute(env);

		assertEquals("https://as.example.com/grants/..%2F..%2Fadmin%3Fx=1", env.getString(GRANT_MANAGEMENT_URL_KEY));
	}

	@Test
	public void setGrantManagementUrl_failsWithoutEndpointMetadata() {
		env.putObject("server", json("{}"));
		env.putString(GRANT_ID_KEY, "abc");

		assertThrows(ConditionError.class,
			() -> cond(new GrantManagementSupport.SetGrantManagementEndpointUrl()).execute(env));
	}

	// ----  discovery checks  ----

	@Test
	public void checkDiscoveryEndpoint_acceptsHttps() {
		env.putObject("server", json("{\"grant_management_endpoint\":\"https://as.example.com/grants\"}"));

		cond(new GrantManagementSupport.CheckDiscoveryForGrantManagementEndpoint()).execute(env);
	}

	@Test
	public void checkDiscoveryEndpoint_rejectsPlainHttp() {
		env.putObject("server", json("{\"grant_management_endpoint\":\"http://as.example.com/grants\"}"));

		assertThrows(ConditionError.class,
			() -> cond(new GrantManagementSupport.CheckDiscoveryForGrantManagementEndpoint()).execute(env));
	}

	@Test
	public void checkDiscoveryEndpoint_rejectsMissing() {
		env.putObject("server", json("{}"));

		assertThrows(ConditionError.class,
			() -> cond(new GrantManagementSupport.CheckDiscoveryForGrantManagementEndpoint()).execute(env));
	}

	@Test
	public void checkActionsSupported_presenceOnly() {
		env.putObject("server", json("{\"grant_management_actions_supported\":[\"query\"]}"));

		// presence alone is enough for the (WARNING-severity) presence check
		cond(new GrantManagementSupport.CheckDiscoveryForGrantManagementActionsSupported()).execute(env);
	}

	@Test
	public void checkActionsSupported_failsWhenAbsent() {
		env.putObject("server", json("{}"));

		assertThrows(ConditionError.class,
			() -> cond(new GrantManagementSupport.CheckDiscoveryForGrantManagementActionsSupported()).execute(env));
	}

	@Test
	public void checkActionsSupportedRequiredActions_acceptsFullSpecEnumeration() {
		env.putObject("server", json("{\"grant_management_actions_supported\":"
			+ "[\"query\",\"revoke\",\"merge\",\"replace\",\"create\"]}"));

		cond(new GrantManagementSupport.CheckDiscoveryForGrantManagementActionsSupportedContainsRequiredActions()).execute(env);
	}

	@Test
	public void checkActionsSupportedRequiredActions_acceptsMinimalRequiredSet() {
		env.putObject("server", json("{\"grant_management_actions_supported\":[\"create\",\"query\",\"revoke\"]}"));

		cond(new GrantManagementSupport.CheckDiscoveryForGrantManagementActionsSupportedContainsRequiredActions()).execute(env);
	}

	@Test
	public void checkActionsSupported_mergeAndReplaceAreCheckedSeparately() {
		env.putObject("server", json("{\"grant_management_actions_supported\":[\"create\",\"query\",\"revoke\"]}"));

		// the generic discovery check is happy without merge/replace ...
		cond(new GrantManagementSupport.CheckDiscoveryForGrantManagementActionsSupportedContainsRequiredActions()).execute(env);

		// ... but the modules that need them are not
		assertThrows(ConditionError.class,
			() -> cond(new GrantManagementSupport.CheckGrantManagementActionsSupportedContainsMerge()).execute(env));
		assertThrows(ConditionError.class,
			() -> cond(new GrantManagementSupport.CheckGrantManagementActionsSupportedContainsReplace()).execute(env));

		env.putObject("server", json("{\"grant_management_actions_supported\":[\"create\",\"query\",\"revoke\",\"merge\",\"replace\"]}"));

		cond(new GrantManagementSupport.CheckGrantManagementActionsSupportedContainsMerge()).execute(env);
		cond(new GrantManagementSupport.CheckGrantManagementActionsSupportedContainsReplace()).execute(env);
	}

	@Test
	public void checkActionsSupportedRequiredActions_failsWhenRevokeMissing() {
		env.putObject("server", json("{\"grant_management_actions_supported\":[\"create\",\"query\"]}"));

		assertThrows(ConditionError.class,
			() -> cond(new GrantManagementSupport.CheckDiscoveryForGrantManagementActionsSupportedContainsRequiredActions()).execute(env));
	}

	// ----  status code assertions  ----

	@Test
	public void querySucceeded_accepts200() {
		env.putObject(GRANT_MANAGEMENT_RESPONSE_KEY, response(200, "{}", "{}"));

		cond(new GrantManagementSupport.EnsureGrantManagementQuerySucceeded()).execute(env);
	}

	@Test
	public void querySucceeded_rejects204() {
		env.putObject(GRANT_MANAGEMENT_RESPONSE_KEY, response(204, "{}", null));

		assertThrows(ConditionError.class,
			() -> cond(new GrantManagementSupport.EnsureGrantManagementQuerySucceeded()).execute(env));
	}

	@Test
	public void revokeSucceeded_accepts204() {
		env.putObject(GRANT_MANAGEMENT_RESPONSE_KEY, response(204, "{}", null));

		cond(new GrantManagementSupport.EnsureGrantManagementRevokeSucceeded()).execute(env);
	}

	@Test
	public void revokeSucceeded_rejects200() {
		env.putObject(GRANT_MANAGEMENT_RESPONSE_KEY, response(200, "{}", "{}"));

		assertThrows(ConditionError.class,
			() -> cond(new GrantManagementSupport.EnsureGrantManagementRevokeSucceeded()).execute(env));
	}

	@Test
	public void returns404_accepts404() {
		env.putObject(GRANT_MANAGEMENT_RESPONSE_KEY, response(404, "{}", null));

		cond(new GrantManagementSupport.EnsureGrantManagementEndpointReturns404()).execute(env);
	}

	@Test
	public void returns404_rejects403() {
		env.putObject(GRANT_MANAGEMENT_RESPONSE_KEY, response(403, "{}", null));

		assertThrows(ConditionError.class,
			() -> cond(new GrantManagementSupport.EnsureGrantManagementEndpointReturns404()).execute(env));
	}

	@Test
	public void returns403Or404_acceptsBoth() {
		env.putObject(GRANT_MANAGEMENT_RESPONSE_KEY, response(403, "{}", null));
		cond(new GrantManagementSupport.EnsureGrantManagementEndpointReturns403Or404()).execute(env);

		env.putObject(GRANT_MANAGEMENT_RESPONSE_KEY, response(404, "{}", null));
		cond(new GrantManagementSupport.EnsureGrantManagementEndpointReturns403Or404()).execute(env);
	}

	@Test
	public void returns403Or404_rejects200() {
		env.putObject(GRANT_MANAGEMENT_RESPONSE_KEY, response(200, "{}", "{}"));

		assertThrows(ConditionError.class,
			() -> cond(new GrantManagementSupport.EnsureGrantManagementEndpointReturns403Or404()).execute(env));
	}

	// ----  query response headers  ----

	@Test
	public void queryContentType_acceptsJsonWithCharset() {
		env.putObject(GRANT_MANAGEMENT_RESPONSE_KEY,
			response(200, "{\"content-type\":\"application/json; charset=UTF-8\"}", "{}"));

		cond(new GrantManagementSupport.EnsureGrantManagementQueryResponseContentTypeIsJson()).execute(env);
	}

	@Test
	public void queryContentType_rejectsHtml() {
		env.putObject(GRANT_MANAGEMENT_RESPONSE_KEY,
			response(200, "{\"content-type\":\"text/html\"}", "{}"));

		assertThrows(ConditionError.class,
			() -> cond(new GrantManagementSupport.EnsureGrantManagementQueryResponseContentTypeIsJson()).execute(env));
	}

	@Test
	public void queryCacheControl_acceptsNoStore() {
		env.putObject(GRANT_MANAGEMENT_RESPONSE_KEY,
			response(200, "{\"cache-control\":\"no-cache, no-store\"}", "{}"));

		cond(new GrantManagementSupport.EnsureGrantManagementQueryResponseIsNotCacheable()).execute(env);
	}

	@Test
	public void queryCacheControl_failsWhenOnlyNoCache() {
		env.putObject(GRANT_MANAGEMENT_RESPONSE_KEY,
			response(200, "{\"cache-control\":\"no-cache\"}", "{}"));

		assertThrows(ConditionError.class,
			() -> cond(new GrantManagementSupport.EnsureGrantManagementQueryResponseIsNotCacheable()).execute(env));
	}

	@Test
	public void queryCacheControl_failsWhenHeaderAbsent() {
		env.putObject(GRANT_MANAGEMENT_RESPONSE_KEY, response(200, "{}", "{}"));

		assertThrows(ConditionError.class,
			() -> cond(new GrantManagementSupport.EnsureGrantManagementQueryResponseIsNotCacheable()).execute(env));
	}

	// ----  query response body validation  ----

	@Test
	public void queryResponseBody_acceptsJsonObject() {
		env.putObject(GRANT_MANAGEMENT_RESPONSE_KEY,
			response(200, "{}", "{\"scopes\":[{\"scope\":\"openid email\"}]}"));

		cond(new GrantManagementSupport.EnsureGrantManagementQueryResponseBodyIsJsonObject()).execute(env);
	}

	@Test
	public void queryResponseBody_failsWhenBodyIsNotJson() {
		env.putObject(GRANT_MANAGEMENT_RESPONSE_KEY, response(200, "{}", null));

		assertThrows(ConditionError.class,
			() -> cond(new GrantManagementSupport.EnsureGrantManagementQueryResponseBodyIsJsonObject()).execute(env));
	}

	@Test
	public void queryResponseGrantDetails_acceptsAnyOneOfTheThreeMembers() {
		for (String body : new String[] {
			"{\"scopes\":[{\"scope\":\"openid email\"}]}",
			"{\"claims\":[\"email\"]}",
			"{\"authorization_details\":[{\"type\":\"account_information\"}]}" }) {

			env.putObject(GRANT_MANAGEMENT_RESPONSE_KEY, response(200, "{}", body));

			cond(new GrantManagementSupport.CheckGrantManagementQueryResponseContainsGrantDetails()).execute(env);
		}
	}

	@Test
	public void queryResponseGrantDetails_flagsResponseDescribingNothing() {
		// all members are OPTIONAL per GM 6.4, so callers run this at WARNING; the condition still throws
		env.putObject(GRANT_MANAGEMENT_RESPONSE_KEY, response(200, "{}", "{\"created_at\":1356123600}"));

		assertThrows(ConditionError.class,
			() -> cond(new GrantManagementSupport.CheckGrantManagementQueryResponseContainsGrantDetails()).execute(env));
	}

	// ----  ValidateGrantManagementQueryResponseTimestamps  ----

	private static String timestamps(long createdAt, long lastUpdatedAt, long expiresAt) {
		return "{\"claims\":[\"email\"],\"created_at\":" + createdAt
			+ ",\"last_updated_at\":" + lastUpdatedAt
			+ ",\"expires_at\":" + expiresAt + "}";
	}

	private void putQueryResponseBody(String bodyJson) {
		env.putObject(GRANT_MANAGEMENT_RESPONSE_KEY, response(200, "{}", bodyJson));
	}

	@Test
	public void queryResponseTimestamps_acceptsPlausibleValues() {
		long now = Instant.now().getEpochSecond();
		putQueryResponseBody("{\"scopes\":[{\"scope\":\"openid\"}],\"claims\":[\"email\"],"
			+ "\"authorization_details\":[{\"type\":\"account_information\"}],"
			+ "\"created_at\":" + (now - 600) + ",\"last_updated_at\":" + (now - 60)
			+ ",\"expires_at\":" + (now + 86400) + ",\"updated_by\":\"client\"}");

		cond(new GrantManagementSupport.ValidateGrantManagementQueryResponseTimestamps()).execute(env);
	}

	@Test
	public void queryResponseTimestamps_acceptsAnEmptyBody() {
		// every member is OPTIONAL, so there is nothing to validate
		putQueryResponseBody("{}");

		cond(new GrantManagementSupport.ValidateGrantManagementQueryResponseTimestamps()).execute(env);
	}

	@Test
	public void queryResponseSchema_rejectsScopesThatAreNotAnArray() {
		putQueryResponseBody("{\"scopes\":\"openid email\"}");

		assertThrows(ConditionError.class,
			() -> cond(new GrantManagementSupport.ValidateGrantManagementQueryResponseSchema()).execute(env));
	}

	/** GM 6.4: "every JSON object may contain a scope field ... and may contain a resource field". */
	@Test
	public void queryResponseSchema_acceptsScopeEntriesWithoutAScope() {
		putQueryResponseBody("{\"scopes\":[{\"resource\":[\"https://rs.example.com\"]}]}");

		cond(new GrantManagementSupport.ValidateGrantManagementQueryResponseSchema()).execute(env);
	}

	@Test
	public void queryResponseSchema_rejectsClaimsThatAreNotStrings() {
		putQueryResponseBody("{\"claims\":[{\"email\":true}]}");

		assertThrows(ConditionError.class,
			() -> cond(new GrantManagementSupport.ValidateGrantManagementQueryResponseSchema()).execute(env));
	}

	@Test
	public void queryResponseSchema_acceptsTheStructureTheSpecDefines() {
		long now = Instant.now().getEpochSecond();
		putQueryResponseBody("{\"scopes\":[{\"scope\":\"openid\",\"resource\":[\"https://rs.example.com\"]}],"
			+ "\"claims\":[\"email\"],\"authorization_details\":[{\"type\":\"account_information\"}],"
			+ "\"created_at\":" + (now - 600) + ",\"updated_by\":\"client\"}");

		cond(new GrantManagementSupport.ValidateGrantManagementQueryResponseSchema()).execute(env);
	}

	@Test
	public void queryResponseTimestamps_rejectsCreatedAtInTheFuture() {
		long now = Instant.now().getEpochSecond();
		putQueryResponseBody(timestamps(now + 3600, now + 3600, now + 86400));

		assertThrows(ConditionError.class,
			() -> cond(new GrantManagementSupport.ValidateGrantManagementQueryResponseTimestamps()).execute(env));
	}

	@Test
	public void queryResponseTimestamps_rejectsExpiresAtInThePast() {
		long now = Instant.now().getEpochSecond();
		putQueryResponseBody(timestamps(now - 600, now - 60, now - 3600));

		assertThrows(ConditionError.class,
			() -> cond(new GrantManagementSupport.ValidateGrantManagementQueryResponseTimestamps()).execute(env));
	}

	@Test
	public void queryResponseTimestamps_rejectsTimestampBeforeTheJwtEra() {
		long now = Instant.now().getEpochSecond();
		// milliseconds mistaken for seconds would land far in the future; seconds mistaken for a small
		// number lands here
		putQueryResponseBody(timestamps(12345, now - 60, now + 86400));

		assertThrows(ConditionError.class,
			() -> cond(new GrantManagementSupport.ValidateGrantManagementQueryResponseTimestamps()).execute(env));
	}

	@Test
	public void queryResponseTimestamps_rejectsTimestampImplausiblyFarInTheFuture() {
		long now = Instant.now().getEpochSecond();
		// a NumericDate expressed in milliseconds
		putQueryResponseBody(timestamps(now - 600, now - 60, now * 1000));

		assertThrows(ConditionError.class,
			() -> cond(new GrantManagementSupport.ValidateGrantManagementQueryResponseTimestamps()).execute(env));
	}

	@Test
	public void queryResponseTimestamps_rejectsLastUpdatedBeforeCreated() {
		long now = Instant.now().getEpochSecond();
		putQueryResponseBody(timestamps(now - 60, now - 600, now + 86400));

		assertThrows(ConditionError.class,
			() -> cond(new GrantManagementSupport.ValidateGrantManagementQueryResponseTimestamps()).execute(env));
	}

	@Test
	public void queryResponseTimestamps_rejectsTimestampThatIsNotANumber() {
		putQueryResponseBody("{\"created_at\":\"2026-01-01T00:00:00Z\"}");

		assertThrows(ConditionError.class,
			() -> cond(new GrantManagementSupport.ValidateGrantManagementQueryResponseTimestamps()).execute(env));
	}

	@Test
	public void queryResponseSchema_rejectsUnknownUpdatedByValue() {
		putQueryResponseBody("{\"claims\":[\"email\"],\"updated_by\":\"resource_owner\"}");

		assertThrows(ConditionError.class,
			() -> cond(new GrantManagementSupport.ValidateGrantManagementQueryResponseSchema()).execute(env));
	}

	@Test
	public void queryResponseSchema_acceptsBothUpdatedByValues() {
		for (String updatedBy : new String[] { "client", "authorization_server" }) {
			putQueryResponseBody("{\"claims\":[\"email\"],\"updated_by\":\"" + updatedBy + "\"}");

			cond(new GrantManagementSupport.ValidateGrantManagementQueryResponseSchema()).execute(env);
		}
	}

	// ----  CheckForUnexpectedParametersInGrantManagementQueryResponse  ----

	@Test
	public void queryResponseMembers_acceptsEveryMemberTheSpecDefines() {
		long now = Instant.now().getEpochSecond();
		putQueryResponseBody("{\"scopes\":[],\"claims\":[],\"authorization_details\":[],"
			+ "\"created_at\":" + (now - 600) + ",\"last_updated_at\":" + (now - 60)
			+ ",\"expires_at\":" + (now + 86400) + ",\"updated_by\":\"client\"}");

		cond(new GrantManagementSupport.CheckForUnexpectedParametersInGrantManagementQueryResponse()).execute(env);
	}

	@Test
	public void queryResponseMembers_flagsTypoedMember() {
		// 'scope' instead of 'scopes' is the mistake this check exists to catch
		putQueryResponseBody("{\"scope\":[{\"scope\":\"openid\"}]}");

		assertThrows(ConditionError.class,
			() -> cond(new GrantManagementSupport.CheckForUnexpectedParametersInGrantManagementQueryResponse()).execute(env));
	}

	@Test
	public void queryResponseMembers_flagsTypoedMemberNestedInScopes() {
		putQueryResponseBody("{\"scopes\":[{\"scope\":\"openid\",\"resources\":[\"https://rs.example.com\"]}]}");

		assertThrows(ConditionError.class,
			() -> cond(new GrantManagementSupport.CheckForUnexpectedParametersInGrantManagementQueryResponse()).execute(env));
	}

	@Test
	public void queryResponseSchema_treatsUnknownMembersAsSomeoneElsesProblem() {
		// the structural validator must stay silent about unknown members, so the caller can report them
		// as a warning rather than failing the test
		putQueryResponseBody("{\"scopes\":[{\"scope\":\"openid\"}],\"an_extension\":true}");

		cond(new GrantManagementSupport.ValidateGrantManagementQueryResponseSchema()).execute(env);
	}

	// ----  DPoP proof for the grant management endpoint  ----

	@Test
	public void dpopHtmHtu_usesTheGrantResourceUrlAndTheRightMethod() {
		env.putObject("dpop_proof_claims", json("{}"));
		env.putString(GRANT_MANAGEMENT_URL_KEY, "https://as.example.com/grants/abc");

		cond(new GrantManagementSupport.SetDpopHtmHtuForGrantManagementQuery()).execute(env);

		assertEquals("GET", env.getString("dpop_proof_claims", "htm"));
		assertEquals("https://as.example.com/grants/abc", env.getString("dpop_proof_claims", "htu"));

		cond(new GrantManagementSupport.SetDpopHtmHtuForGrantManagementRevoke()).execute(env);

		assertEquals("DELETE", env.getString("dpop_proof_claims", "htm"));
		assertEquals("https://as.example.com/grants/abc", env.getString("dpop_proof_claims", "htu"));
	}

	// ----  invalid_grant_id error handling  ----

	@Test
	public void authorizationEndpointRejectsInvalidGrantId_acceptsExpectedError() {
		env.putObject("authorization_endpoint_response", json("{\"error\":\"invalid_grant_id\"}"));

		cond(new GrantManagementSupport.EnsureAuthorizationEndpointRejectsInvalidGrantId()).execute(env);
	}

	@Test
	public void authorizationEndpointRejectsInvalidGrantId_failsOnDifferentError() {
		env.putObject("authorization_endpoint_response", json("{\"error\":\"invalid_request\"}"));

		assertThrows(ConditionError.class,
			() -> cond(new GrantManagementSupport.EnsureAuthorizationEndpointRejectsInvalidGrantId()).execute(env));
	}

	@Test
	public void parEndpointRejectsInvalidGrantId_acceptsExpectedError() {
		env.putObject("endpoint_response", response(400, "{}", "{\"error\":\"invalid_grant_id\"}"));

		cond(new GrantManagementSupport.EnsurePAREndpointRejectsInvalidGrantId()).execute(env);
	}

	@Test
	public void parEndpointRejectsInvalidGrantId_failsWhenErrorAbsent() {
		env.putObject("endpoint_response", response(400, "{}", "{}"));

		assertThrows(ConditionError.class,
			() -> cond(new GrantManagementSupport.EnsurePAREndpointRejectsInvalidGrantId()).execute(env));
	}

	// ----  RP-side conditions  ----

	@Test
	public void parRequestGrantManagementAction_acceptsFormParameter() {
		env.putObject("par_endpoint_http_request_params", json("{\"grant_management_action\":\"create\"}"));

		cond(new GrantManagementSupport.EnsurePARRequestContainsGrantManagementAction()).execute(env);
	}

	@Test
	public void parRequestGrantManagementAction_acceptsRequestObjectClaim() {
		env.putObject("par_endpoint_http_request_params", json("{}"));
		env.putObject("authorization_request_object", json("{\"claims\":{\"grant_management_action\":\"merge\"}}"));

		cond(new GrantManagementSupport.EnsurePARRequestContainsGrantManagementAction()).execute(env);
	}

	@Test
	public void parRequestGrantManagementAction_failsWhenClientDidNotUseGrantManagement() {
		env.putObject("par_endpoint_http_request_params", json("{\"scope\":\"openid\"}"));

		assertThrows(ConditionError.class,
			() -> cond(new GrantManagementSupport.EnsurePARRequestContainsGrantManagementAction()).execute(env));
	}

	@Test
	public void parRequestGrantManagementAction_failsOnActionTheSpecDoesNotDefine() {
		// 'update' is a common mistake; GM 5.2 defines create, merge and replace
		env.putObject("par_endpoint_http_request_params", json("{\"grant_management_action\":\"update\"}"));

		assertThrows(ConditionError.class,
			() -> cond(new GrantManagementSupport.EnsurePARRequestContainsGrantManagementAction()).execute(env));
	}

	@Test
	public void parRequestGrantId_acceptsGrantIdFromRequestObject() {
		env.putObject("par_endpoint_http_request_params", json("{}"));
		env.putObject("authorization_request_object", json("{\"claims\":{\"grant_id\":\"TSdqirmAxDa0_-DB_1bASQ\"}}"));

		cond(new GrantManagementSupport.EnsurePARRequestContainsGrantId()).execute(env);
	}

	@Test
	public void parRequestGrantId_failsWhenAbsent() {
		env.putObject("par_endpoint_http_request_params", json("{\"grant_management_action\":\"merge\"}"));

		assertThrows(ConditionError.class,
			() -> cond(new GrantManagementSupport.EnsurePARRequestContainsGrantId()).execute(env));
	}

	@Test
	public void parRequestGrantIdWithCreate_acceptsCreateWithoutGrantId() {
		env.putObject("par_endpoint_http_request_params", json("{\"grant_management_action\":\"create\"}"));

		cond(new GrantManagementSupport.EnsurePARRequestDoesNotContainGrantIdWithCreateAction()).execute(env);
	}

	@Test
	public void parRequestGrantIdWithCreate_failsWhenGrantIdSentWithCreate() {
		env.putObject("par_endpoint_http_request_params",
			json("{\"grant_management_action\":\"create\",\"grant_id\":\"TSdqirmAxDa0_-DB_1bASQ\"}"));

		assertThrows(ConditionError.class,
			() -> cond(new GrantManagementSupport.EnsurePARRequestDoesNotContainGrantIdWithCreateAction()).execute(env));
	}

	@Test
	public void parRequestGrantIdWithCreate_allowsGrantIdWithMerge() {
		env.putObject("par_endpoint_http_request_params",
			json("{\"grant_management_action\":\"merge\",\"grant_id\":\"TSdqirmAxDa0_-DB_1bASQ\"}"));

		cond(new GrantManagementSupport.EnsurePARRequestDoesNotContainGrantIdWithCreateAction()).execute(env);
	}

	@Test
	public void addGrantManagementToServerConfiguration_advertisesEndpointAndActions() {
		env.putObject("server", json("{\"token_endpoint\":\"https://suite.example.com/test/a/x/token\","
			+ "\"mtls_endpoint_aliases\":{\"token_endpoint\":\"https://suite-mtls.example.com/test/a/x/token\"}}"));

		cond(new GrantManagementSupport.AddGrantManagementToServerConfiguration()).execute(env);

		assertEquals("https://suite.example.com/test/a/x/grants",
			env.getString("server", "grant_management_endpoint"));
		assertEquals("https://suite-mtls.example.com/test/a/x/grants",
			env.getString("server", "mtls_endpoint_aliases.grant_management_endpoint"));

		// a client that discovers this must be able to see that merge and replace are available
		cond(new GrantManagementSupport.CheckDiscoveryForGrantManagementActionsSupportedContainsRequiredActions()).execute(env);
		cond(new GrantManagementSupport.CheckGrantManagementActionsSupportedContainsMerge()).execute(env);
		cond(new GrantManagementSupport.CheckGrantManagementActionsSupportedContainsReplace()).execute(env);
	}

	@Test
	public void grantManagementRequestForIssuedGrant_acceptsTheGrantWeIssued() {
		env.putString(GRANT_ID_KEY, "TSdqirmAxDa0_-DB_1bASQ");
		env.putString(GrantManagementSupport.EnsureGrantManagementRequestIsForIssuedGrant.REQUESTED_GRANT_ID,
			"TSdqirmAxDa0_-DB_1bASQ");

		cond(new GrantManagementSupport.EnsureGrantManagementRequestIsForIssuedGrant()).execute(env);
	}

	@Test
	public void grantManagementRequestForIssuedGrant_rejectsAnyOtherGrantId() {
		env.putString(GRANT_ID_KEY, "TSdqirmAxDa0_-DB_1bASQ");
		env.putString(GrantManagementSupport.EnsureGrantManagementRequestIsForIssuedGrant.REQUESTED_GRANT_ID,
			"some-other-grant");

		assertThrows(ConditionError.class,
			() -> cond(new GrantManagementSupport.EnsureGrantManagementRequestIsForIssuedGrant()).execute(env));
	}

	@Test
	public void createQueryResponse_describesTheGrantAndIsNotCacheable() {
		env.putObject("client", json("{\"scope\":\"openid accounts\"}"));

		cond(new GrantManagementSupport.CreateGrantManagementQueryResponse()).execute(env);

		JsonObject response = env.getObject(GrantManagementSupport.CreateGrantManagementQueryResponse.RESPONSE_KEY);
		assertEquals("openid accounts",
			OIDFJSON.getString(response.getAsJsonArray("scopes").get(0).getAsJsonObject().get("scope")));
		assertEquals("client", OIDFJSON.getString(response.get("updated_by")));
		assertEquals("no-cache, no-store",
			env.getString(GrantManagementSupport.CreateGrantManagementQueryResponse.RESPONSE_HEADERS_KEY, "Cache-Control"));

		// what the suite serves must itself satisfy the checks it applies to a real server
		env.putObject(GRANT_MANAGEMENT_RESPONSE_KEY, response(200, "{\"cache-control\":\"no-cache, no-store\"}", response.toString()));
		cond(new GrantManagementSupport.EnsureGrantManagementQuerySucceeded()).execute(env);
		cond(new GrantManagementSupport.EnsureGrantManagementQueryResponseIsNotCacheable()).execute(env);
		cond(new GrantManagementSupport.ValidateGrantManagementQueryResponseSchema()).execute(env);
		cond(new GrantManagementSupport.ValidateGrantManagementQueryResponseTimestamps()).execute(env);
		cond(new GrantManagementSupport.CheckGrantManagementQueryResponseContainsGrantDetails()).execute(env);
		cond(new GrantManagementSupport.CheckForUnexpectedParametersInGrantManagementQueryResponse()).execute(env);
	}

	@Test
	public void createQueryResponse_omitsScopesWhenTheClientHasNone() {
		env.putObject("client", json("{\"client_id\":\"client1\"}"));

		cond(new GrantManagementSupport.CreateGrantManagementQueryResponse()).execute(env);

		JsonObject grant = env.getObject(GrantManagementSupport.CreateGrantManagementQueryResponse.RESPONSE_KEY);
		assertNull(grant.get("scopes"));
		assertNotNull(grant.get("created_at"));
	}

	@Test
	public void createPARInvalidGrantIdErrorResponse_setsBodyHeadersAndStatus() {
		cond(new GrantManagementSupport.CreatePAREndpointInvalidGrantIdErrorResponse()).execute(env);

		assertEquals("invalid_grant_id", env.getString("par_endpoint_response", "error"));
		assertEquals("application/json", env.getString("par_endpoint_response_headers", "Content-Type"));
		assertEquals(400, env.getInteger("par_endpoint_response_http_status"));
	}

	@Test
	public void addGrantIdToTokenEndpointResponse_generatesHighEntropyGrantId() {
		env.putObject("token_endpoint_response", json("{\"access_token\":\"abc\"}"));

		cond(new GrantManagementSupport.AddGrantIdToTokenEndpointResponse()).execute(env);

		String grantId = env.getString(GRANT_ID_KEY);
		assertNotNull(grantId);
		assertTrue(grantId.length() >= 20, "generated grant_id must satisfy the entropy check");
		cond(new GrantManagementSupport.CheckGrantIdIsUrlSafe()).execute(env);
		cond(new GrantManagementSupport.CheckGrantIdHasSufficientEntropy()).execute(env);
		assertEquals(grantId,
			OIDFJSON.getString(env.getObject("token_endpoint_response").get("grant_id")));
	}

	@Test
	public void addGrantIdToTokenEndpointResponse_thenExtractRoundTrips() {
		env.putObject("token_endpoint_response", json("{\"access_token\":\"abc\"}"));
		cond(new GrantManagementSupport.AddGrantIdToTokenEndpointResponse()).execute(env);
		String generated = env.getString(GRANT_ID_KEY);

		cond(new GrantManagementSupport.ExtractGrantIdFromTokenResponse()).execute(env);

		assertEquals(generated, env.getString(GRANT_ID_KEY));
	}

	// ----  last_updated / last_updated_at (GM 6.4 prose vs examples)  ----

	/** GM 6.4's prose names the member 'last_updated' while its examples use 'last_updated_at'. */
	@Test
	public void queryResponseTimestamps_acceptsTheNameUsedByTheSpecProse() {
		long now = Instant.now().getEpochSecond();
		putQueryResponseBody("{\"created_at\":" + (now - 600) + ",\"last_updated\":" + (now - 60)
			+ ",\"expires_at\":" + (now + 86400) + "}");

		cond(new GrantManagementSupport.ValidateGrantManagementQueryResponseTimestamps()).execute(env);
	}

	@Test
	public void queryResponseTimestamps_validatesTheProseNameToo() {
		long now = Instant.now().getEpochSecond();
		putQueryResponseBody("{\"created_at\":" + (now - 60) + ",\"last_updated\":" + (now - 600) + "}");

		assertThrows(ConditionError.class,
			() -> cond(new GrantManagementSupport.ValidateGrantManagementQueryResponseTimestamps()).execute(env));
	}

	@Test
	public void queryResponseTimestamps_flagsBothNamesAtOnce() {
		long now = Instant.now().getEpochSecond();
		putQueryResponseBody("{\"created_at\":" + (now - 600) + ",\"last_updated\":" + (now - 60)
			+ ",\"last_updated_at\":" + (now - 60) + "}");

		assertThrows(ConditionError.class,
			() -> cond(new GrantManagementSupport.ValidateGrantManagementQueryResponseTimestamps()).execute(env));
	}

	@Test
	public void queryResponseSchema_acceptsTheProseTimestampName() {
		putQueryResponseBody("{\"last_updated\":1356123600}");

		cond(new GrantManagementSupport.ValidateGrantManagementQueryResponseSchema()).execute(env);
		cond(new GrantManagementSupport.CheckForUnexpectedParametersInGrantManagementQueryResponse()).execute(env);
	}

	// ----  status handling (GM 6.4 / 6.5)  ----

	@Test
	public void querySucceeded_reportsAnUnexpectedStatus() {
		env.putObject(GRANT_MANAGEMENT_RESPONSE_KEY, response(404, "{}", null));

		ConditionError error = assertThrows(ConditionError.class,
			() -> cond(new GrantManagementSupport.EnsureGrantManagementQuerySucceeded()).execute(env));
		assertTrue(error.getMessage().contains("Expected HTTP 200"), error.getMessage());
	}

	@Test
	public void dpopNonceRetryCondition_alwaysReportsTheGiveUp() {
		env.putObject(GRANT_MANAGEMENT_RESPONSE_KEY, response(401, "{}", null));

		ConditionError error = assertThrows(ConditionError.class,
			() -> cond(new GrantManagementSupport.EnsureGrantManagementDpopNonceRetryWasAccepted()).execute(env));
		assertTrue(error.getMessage().contains("use_dpop_nonce"), error.getMessage());
	}

	@Test
	public void revokeBodyCheck_acceptsAnEmptyBody() {
		env.putObject(GRANT_MANAGEMENT_RESPONSE_KEY, response(204, "{}", null));

		cond(new GrantManagementSupport.EnsureGrantManagementRevokeResponseBodyIsEmpty()).execute(env);
	}

	@Test
	public void revokeBodyCheck_flagsANonEmptyBody() {
		JsonObject revokeResponse = response(204, "{}", null);
		revokeResponse.addProperty("body", "{\"revoked\":true}");
		env.putObject(GRANT_MANAGEMENT_RESPONSE_KEY, revokeResponse);

		assertThrows(ConditionError.class,
			() -> cond(new GrantManagementSupport.EnsureGrantManagementRevokeResponseBodyIsEmpty()).execute(env));
	}

	// ----  mtls_endpoint_aliases (RFC8705-5)  ----

	@Test
	public void setGrantManagementUrl_prefersTheFlattenedMtlsAliasWhenMtlsIsInUse() {
		env.putObject("server", json("{\"grant_management_endpoint\":\"https://as.example.com/grants\"}"));
		// AddMTLSEndpointAliasesToEnvironment flattens the aliased value into the root of the environment
		env.putString("grant_management_endpoint", "https://mtls.as.example.com/grants");
		env.putObject("mutual_tls_authentication", json("{}"));
		env.putString(GRANT_ID_KEY, "abc");

		cond(new GrantManagementSupport.SetGrantManagementEndpointUrl()).execute(env);

		assertEquals("https://mtls.as.example.com/grants/abc", env.getString(GRANT_MANAGEMENT_URL_KEY));
	}

	@Test
	public void setGrantManagementUrl_ignoresTheFlattenedValueWithoutMtls() {
		env.putObject("server", json("{\"grant_management_endpoint\":\"https://as.example.com/grants\"}"));
		env.putString("grant_management_endpoint", "https://mtls.as.example.com/grants");
		env.putString(GRANT_ID_KEY, "abc");

		cond(new GrantManagementSupport.SetGrantManagementEndpointUrl()).execute(env);

		assertEquals("https://as.example.com/grants/abc", env.getString(GRANT_MANAGEMENT_URL_KEY));
	}

	// ----  granted scope coverage (GM 6.4)  ----

	@Test
	public void coversGrantedScope_ignoresTheGrantManagementApiScopes() {
		// GM 6.1 defines these as scopes for the grant management API, not permissions in the grant, so an
		// AS that echoes them in the granted scope must not be required to list them in the grant
		env.putObject("client", json("{\"granted_scope\":\"openid payments grant_management_query grant_management_revoke\"}"));
		putQueryResponseBody("{\"scopes\":[{\"scope\":\"openid payments\"}]}");

		cond(new GrantManagementSupport.CheckGrantManagementQueryResponseCoversGrantedScope()).execute(env);
	}

	@Test
	public void coversGrantedScope_stillFlagsAMissingGrantedScope() {
		env.putObject("client", json("{\"granted_scope\":\"openid payments grant_management_query\"}"));
		putQueryResponseBody("{\"scopes\":[{\"scope\":\"openid\"}]}");

		assertThrows(ConditionError.class,
			() -> cond(new GrantManagementSupport.CheckGrantManagementQueryResponseCoversGrantedScope()).execute(env));
	}

	// ----  choosing an action that carries a grant_id (GM 5.4 / 7.1)  ----

	@Test
	public void selectActionTakingAGrantId_prefersMergeWhenAdvertised() {
		env.putObject("server", json("{\"grant_management_actions_supported\":[\"create\",\"query\",\"revoke\",\"merge\",\"replace\"]}"));
		env.putObject("authorization_endpoint_request", json("{}"));

		cond(new GrantManagementSupport.SelectGrantManagementActionTakingAGrantId()).execute(env);

		assertEquals("merge", env.getString(GrantManagementSupport.SELECTED_GRANT_MANAGEMENT_ACTION_KEY));
		assertEquals("merge", env.getString("authorization_endpoint_request", "grant_management_action"));
	}

	@Test
	public void selectActionTakingAGrantId_fallsBackToReplace() {
		env.putObject("server", json("{\"grant_management_actions_supported\":[\"create\",\"query\",\"revoke\",\"replace\"]}"));
		env.putObject("authorization_endpoint_request", json("{}"));

		cond(new GrantManagementSupport.SelectGrantManagementActionTakingAGrantId()).execute(env);

		assertEquals("replace", env.getString(GrantManagementSupport.SELECTED_GRANT_MANAGEMENT_ACTION_KEY));
	}

	@Test
	public void selectActionTakingAGrantId_leavesTheKeyUnsetWhenNeitherIsSupported() {
		env.putObject("server", json("{\"grant_management_actions_supported\":[\"create\",\"query\",\"revoke\"]}"));
		env.putObject("authorization_endpoint_request", json("{}"));

		cond(new GrantManagementSupport.SelectGrantManagementActionTakingAGrantId()).execute(env);

		assertNull(env.getString(GrantManagementSupport.SELECTED_GRANT_MANAGEMENT_ACTION_KEY));
		assertNull(env.getString("authorization_endpoint_request", "grant_management_action"));
	}

	// ----  the RP must update a grant, not create one (GM 5.4)  ----

	@Test
	public void parActionUpdatingAGrant_acceptsMergeAndReplace() {
		env.putObject("par_endpoint_http_request_params", json("{\"grant_management_action\":\"merge\"}"));
		cond(new GrantManagementSupport.EnsurePARRequestContainsGrantManagementActionUpdatingAGrant()).execute(env);

		env.putObject("par_endpoint_http_request_params", json("{\"grant_management_action\":\"replace\"}"));
		cond(new GrantManagementSupport.EnsurePARRequestContainsGrantManagementActionUpdatingAGrant()).execute(env);
	}

	@Test
	public void parActionUpdatingAGrant_rejectsCreate() {
		env.putObject("par_endpoint_http_request_params", json("{\"grant_management_action\":\"create\"}"));

		assertThrows(ConditionError.class,
			() -> cond(new GrantManagementSupport.EnsurePARRequestContainsGrantManagementActionUpdatingAGrant()).execute(env));
	}

	// ----  merge / replace keep the same grant  ----

	@Test
	public void grantIdComparison_passesWhenTheSameGrantIsReturned() {
		env.putString(GRANT_ID_KEY, "abcdefghijklmnopqrst");
		cond(new GrantManagementSupport.StoreGrantIdForComparison()).execute(env);

		cond(new GrantManagementSupport.EnsureGrantIdIsUnchanged()).execute(env);
	}

	@Test
	public void grantIdComparison_flagsANewlyMintedGrant() {
		env.putString(GRANT_ID_KEY, "abcdefghijklmnopqrst");
		cond(new GrantManagementSupport.StoreGrantIdForComparison()).execute(env);
		env.putString(GRANT_ID_KEY, "a-completely-different-grant");

		assertThrows(ConditionError.class,
			() -> cond(new GrantManagementSupport.EnsureGrantIdIsUnchanged()).execute(env));
	}

	@Test
	public void fabricatedGrantId_isRandomAndUrlSafe() {
		cond(new GrantManagementSupport.CreateGrantIdThatDoesNotExist()).execute(env);
		String first = env.getString(GRANT_ID_KEY);

		cond(new GrantManagementSupport.CreateGrantIdThatDoesNotExist()).execute(env);

		assertNotEquals(first, env.getString(GRANT_ID_KEY));
		env.putString(GRANT_ID_KEY, first);
		cond(new GrantManagementSupport.CheckGrantIdIsUrlSafe()).execute(env);
		cond(new GrantManagementSupport.CheckGrantIdHasSufficientEntropy()).execute(env);
	}

	// ----  the grant covers what the token endpoint granted  ----

	@Test
	public void grantCoversGrantedScope_passesWhenEveryGrantedScopeIsPresent() {
		env.putObject("client", json("{\"granted_scope\":\"openid payments\"}"));
		putQueryResponseBody("{\"scopes\":[{\"scope\":\"openid\"},{\"scope\":\"payments accounts\"}]}");

		cond(new GrantManagementSupport.CheckGrantManagementQueryResponseCoversGrantedScope()).execute(env);
	}

	@Test
	public void grantCoversGrantedScope_flagsAScopeMissingFromTheGrant() {
		env.putObject("client", json("{\"granted_scope\":\"openid payments\"}"));
		putQueryResponseBody("{\"scopes\":[{\"scope\":\"openid\"}]}");

		assertThrows(ConditionError.class,
			() -> cond(new GrantManagementSupport.CheckGrantManagementQueryResponseCoversGrantedScope()).execute(env));
	}

	/** Every member of the query response is OPTIONAL per GM 6.4, so there may be nothing to compare. */
	@Test
	public void grantCoversGrantedScope_acceptsAResponseWithoutScopes() {
		env.putObject("client", json("{\"granted_scope\":\"openid\"}"));
		putQueryResponseBody("{\"claims\":[\"email\"]}");

		cond(new GrantManagementSupport.CheckGrantManagementQueryResponseCoversGrantedScope()).execute(env);
	}

	@Test
	public void grantCoversGrantedScope_acceptsATokenResponseWithoutAScope() {
		env.putObject("client", json("{}"));
		putQueryResponseBody("{\"scopes\":[{\"scope\":\"openid\"}]}");

		cond(new GrantManagementSupport.CheckGrantManagementQueryResponseCoversGrantedScope()).execute(env);
	}

	// ----  the emulated OP's grant management endpoint  ----

	@Test
	public void serverConfiguration_derivesTheGrantEndpointFromTheTokenEndpoint() {
		env.putObject("server", json("{\"token_endpoint\":\"https://as.example.com/oauth/token\","
			+ "\"mtls_endpoint_aliases\":{\"token_endpoint\":\"https://mtls.example.com/oauth/token\"}}"));

		cond(new GrantManagementSupport.AddGrantManagementToServerConfiguration()).execute(env);

		assertEquals("https://as.example.com/oauth/grants",
			env.getString("server", "grant_management_endpoint"));
		assertEquals("https://mtls.example.com/oauth/grants",
			env.getString("server", "mtls_endpoint_aliases.grant_management_endpoint"));
	}

	/** Silently returning the token endpoint itself would be far harder to diagnose than failing here. */
	@Test
	public void serverConfiguration_flagsATokenEndpointItCannotDeriveFrom() {
		env.putObject("server", json("{\"token_endpoint\":\"https://as.example.com/oauth/tok\"}"));

		assertThrows(ConditionError.class,
			() -> cond(new GrantManagementSupport.AddGrantManagementToServerConfiguration()).execute(env));
	}
}
