package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.util.http.WwwAuthenticateHeaderValueParser;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WwwAuthenticateParserTest {

	@Test
	public void extractChallengesFromEmptyAuthenticateChallenges() {

		Map<String,String> challenges = WwwAuthenticateHeaderValueParser.extractChallenges("");
		assertTrue(challenges.isEmpty());
	}

	@Test
	public void extractChallengesFromNullAuthenticateChallenges() {

		Map<String,String> challenges = WwwAuthenticateHeaderValueParser.extractChallenges(null);
		assertTrue(challenges.isEmpty());
	}

	@Test
	public void extractChallengesFromAuthenticateChallengesAsCommaSeparatedList() {

		Map<String,String> challenges = WwwAuthenticateHeaderValueParser.extractChallenges("Newauth realm=\"apps\", type=1, title=\"Login to \\\"apps\\\"\", Basic realm=\"simple\"");

		assertEquals("Newauth realm=\"apps\", type=1, title=\"Login to \\\"apps\\\"\"", challenges.get("Newauth"));
		assertEquals("Basic realm=\"simple\"", challenges.get("Basic"));
	}

	@Test
	public void extractChallengesFromAuthenticateChallengesAsCommaSeparatedListWithDPoP() {

		Map<String,String> challenges = WwwAuthenticateHeaderValueParser.extractChallenges("Bearer realm=\"\", DPoP algs=\"ES256\", error=\"use_dpop_nonce\", error_description=\"Authorization server requires nonce in DPoP proof\"");

		assertEquals("DPoP algs=\"ES256\", error=\"use_dpop_nonce\", error_description=\"Authorization server requires nonce in DPoP proof\"", challenges.get("DPoP"));
		assertEquals("Bearer realm=\"\"", challenges.get("Bearer"));
	}

	@Test
	public void parseMultiAuthenticateChallengesAsCommaSeparatedList() {

		Map<String, Map<String, String>> wwwAuthenticateValue = WwwAuthenticateHeaderValueParser.parse("Newauth realm=\"apps\", type=1, title=\"Login to \\\"apps\\\"\", Basic realm=\"simple\"");

		Map<String, String> newauth = wwwAuthenticateValue.get("Newauth");
		assertEquals("apps", newauth.get("realm"));
		assertEquals("Login to \"apps\"", newauth.get("title"));
	}

	@Test
	public void parseMultiAuthenticateChallengesAsCommaSeparatedListWithDPoP() {

		Map<String, Map<String, String>> wwwAuthenticateValue = WwwAuthenticateHeaderValueParser.parse("Bearer realm=\"\", DPoP algs=\"ES256\", error=\"use_dpop_nonce\", error_description=\"Authorization server requires nonce in DPoP proof\"");

		Map<String, String> dPoP = wwwAuthenticateValue.get("DPoP");
		assertTrue(dPoP.containsKey("error"));
		assertEquals("use_dpop_nonce", dPoP.get("error"));
	}

	@Test
	public void parseMultiAuthenticateChallenges() {
		// 1. Basic
		var basic = WwwAuthenticateHeaderValueParser.parse("Basic realm=\"example\"");
		assertEquals("example", basic.get("Basic").get("realm"));

		// 2. Bearer with error
		var bearer1 = WwwAuthenticateHeaderValueParser.parse("Bearer realm=\"api\", error=\"invalid_token\"");
		assertEquals("api", bearer1.get("Bearer").get("realm"));
		assertEquals("invalid_token", bearer1.get("Bearer").get("error"));

		// 3. Digest
		var digest = WwwAuthenticateHeaderValueParser.parse("Digest realm=\"example\", qop=\"auth\", nonce=\"abc123\", opaque=\"xyz\"");
		var digestMap = digest.get("Digest");
		assertEquals("example", digestMap.get("realm"));
		assertEquals("auth", digestMap.get("qop"));
		assertEquals("abc123", digestMap.get("nonce"));
		assertEquals("xyz", digestMap.get("opaque"));

		// 4. Negotiate
		var negotiate = WwwAuthenticateHeaderValueParser.parse("Negotiate");
		assertTrue(negotiate.containsKey("Negotiate"));

		// 5. NTLM
		var ntlm = WwwAuthenticateHeaderValueParser.parse("NTLM");
		assertTrue(ntlm.containsKey("NTLM"));

		// 6. dummy Newauth
		var newauth = WwwAuthenticateHeaderValueParser.parse("Newauth realm=\"apps\", type=1, title=\"Login to \\\"apps\\\"\"");
		var newauthMap = newauth.get("Newauth");
		assertEquals("apps", newauthMap.get("realm"));
		assertEquals("1", newauthMap.get("type"));
		assertEquals("Login to \"apps\"", newauthMap.get("title"));

		// 7. Bearer with scope and error details
		var bearer2 = WwwAuthenticateHeaderValueParser.parse("Bearer realm=\"example\", scope=\"read write\", error=\"invalid_token\", error_description=\"Access token expired\"");
		var bearer2Map = bearer2.get("Bearer");
		assertEquals("example", bearer2Map.get("realm"));
		assertEquals("read write", bearer2Map.get("scope"));
		assertEquals("invalid_token", bearer2Map.get("error"));
		assertEquals("Access token expired", bearer2Map.get("error_description"));

		// 8. DPoP
		var dpop = WwwAuthenticateHeaderValueParser.parse("DPoP realm=\"example\", algs=\"ES256 PS256\"");
		var dpopMap = dpop.get("DPoP");
		assertEquals("example", dpopMap.get("realm"));
		assertEquals("ES256 PS256", dpopMap.get("algs"));

		// 9. Mutual
		var mutual = WwwAuthenticateHeaderValueParser.parse("Mutual realm=\"secure\", token68");
		var mutualMap = mutual.get("Mutual");
		assertEquals("secure", mutualMap.get("realm"));
		assertTrue(mutualMap.containsKey("token68"));
	}

	private static JsonObject headersWithWwwAuthenticate(JsonElement value) {
		JsonObject headers = new JsonObject();
		headers.add("www-authenticate", value);
		return headers;
	}

	private static JsonObject headersWithWwwAuthenticate(String value) {
		return headersWithWwwAuthenticate(new JsonPrimitive(value));
	}

	@Test
	public void hasUseDpopNonceChallengeWithNoHeaders() {
		assertFalse(WwwAuthenticateHeaderValueParser.hasUseDpopNonceChallenge(null));
		assertFalse(WwwAuthenticateHeaderValueParser.hasUseDpopNonceChallenge(new JsonObject()));
	}

	@Test
	public void hasUseDpopNonceChallengeFromSingleHeaderValue() {
		assertTrue(WwwAuthenticateHeaderValueParser.hasUseDpopNonceChallenge(
			headersWithWwwAuthenticate("DPoP error=\"use_dpop_nonce\", error_description=\"nonce required\"")));
	}

	/**
	 * Servers that combine every challenge into one header value: the DPoP challenge is not the first one.
	 */
	@Test
	public void hasUseDpopNonceChallengeFromCombinedMultiChallengeHeader() {
		assertTrue(WwwAuthenticateHeaderValueParser.hasUseDpopNonceChallenge(
			headersWithWwwAuthenticate("Bearer realm=\"\", DPoP algs=\"ES256\", error=\"use_dpop_nonce\", error_description=\"Authorization server requires nonce in DPoP proof\"")));
	}

	/**
	 * Servers that send the header more than once, which the environment stores as a JSON array.
	 */
	@Test
	public void hasUseDpopNonceChallengeFromHeaderArray() {
		JsonArray values = new JsonArray();
		values.add("Bearer realm=\"example\", error=\"invalid_token\"");
		values.add("DPoP algs=\"ES256\", error=\"use_dpop_nonce\"");

		assertTrue(WwwAuthenticateHeaderValueParser.hasUseDpopNonceChallenge(headersWithWwwAuthenticate(values)));
	}

	/**
	 * Auth schemes are case insensitive per RFC9110 section 11.1.
	 */
	@Test
	public void hasUseDpopNonceChallengeIsCaseInsensitiveForTheScheme() {
		assertTrue(WwwAuthenticateHeaderValueParser.hasUseDpopNonceChallenge(
			headersWithWwwAuthenticate("dpop error=\"use_dpop_nonce\"")));
		assertTrue(WwwAuthenticateHeaderValueParser.hasUseDpopNonceChallenge(
			headersWithWwwAuthenticate("DPOP error=\"use_dpop_nonce\"")));
	}

	@Test
	public void hasUseDpopNonceChallengeIgnoresOtherErrorsAndSchemes() {
		// a DPoP challenge, but not the nonce one
		assertFalse(WwwAuthenticateHeaderValueParser.hasUseDpopNonceChallenge(
			headersWithWwwAuthenticate("DPoP algs=\"ES256\", error=\"invalid_token\"")));
		// the right error, but on the wrong scheme
		assertFalse(WwwAuthenticateHeaderValueParser.hasUseDpopNonceChallenge(
			headersWithWwwAuthenticate("Bearer error=\"use_dpop_nonce\"")));
		// a DPoP challenge with no error at all
		assertFalse(WwwAuthenticateHeaderValueParser.hasUseDpopNonceChallenge(
			headersWithWwwAuthenticate("DPoP algs=\"ES256 PS256\"")));
	}

	/**
	 * The environment is not guaranteed to hold a string here, and a non-string entry must not blow up.
	 */
	@Test
	public void hasUseDpopNonceChallengeIgnoresNonStringHeaderValues() {
		assertFalse(WwwAuthenticateHeaderValueParser.hasUseDpopNonceChallenge(
			headersWithWwwAuthenticate(new JsonObject())));

		JsonArray values = new JsonArray();
		values.add(new JsonObject());
		values.add("DPoP error=\"use_dpop_nonce\"");
		assertTrue(WwwAuthenticateHeaderValueParser.hasUseDpopNonceChallenge(headersWithWwwAuthenticate(values)));
	}
}
