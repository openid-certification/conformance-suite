package net.openid.conformance.condition.client;

import net.openid.conformance.util.http.WwwAuthenticateHeaderValueParser;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
