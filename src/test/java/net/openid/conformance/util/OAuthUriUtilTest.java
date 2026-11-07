package net.openid.conformance.util;

import org.junit.jupiter.api.Test;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OAuthUriUtilTest {

	@Test
	public void formUrlEncodedQueryEncodesPlusAndSpaceDistinctly() {
		Map<String, String> params = new LinkedHashMap<>();
		params.put("scope", "openid profile");
		params.put("format", "dc+sd-jwt");
		params.put("redirect_uri", "https://client.example.com/cb?x=1&y=2");

		String actual = OAuthUriUtil.appendFormUrlEncodedQuery("https://example.com/authorize", params);

		assertEquals("https://example.com/authorize?scope=openid%20profile&format=dc%2Bsd-jwt"
			+ "&redirect_uri=https%3A%2F%2Fclient.example.com%2Fcb%3Fx%3D1%26y%3D2", actual);
		// RFC 6749 appendix B decoding (URLDecoder treats '+' as a space) gives the original values back
		Map<String, String> decoded = new LinkedHashMap<>();
		for (String pair : actual.substring(actual.indexOf('?') + 1).split("&")) {
			String[] nameValue = pair.split("=", 2);
			decoded.put(URLDecoder.decode(nameValue[0], StandardCharsets.UTF_8), URLDecoder.decode(nameValue[1], StandardCharsets.UTF_8));
		}
		assertEquals(params, decoded);
	}

	@Test
	public void formUrlEncodedQueryWithoutUrlHasNoLeadingSeparator() {
		Map<String, String> params = new LinkedHashMap<>();
		params.put("code", "abc");
		params.put("state", "a+b c");
		assertEquals("code=abc&state=a%2Bb%20c", OAuthUriUtil.formUrlEncodedQuery(params));
		assertEquals("", OAuthUriUtil.formUrlEncodedQuery(Map.of()));
	}

	@Test
	public void spaceAsPlusWritesSpacesAsPlusAndPlusesAsPercent2B() {
		Map<String, String> params = new LinkedHashMap<>();
		params.put("scope", "openid email");
		params.put("state", "a+b c");
		assertEquals("https://example.com/authorize?scope=openid+email&state=a%2Bb+c",
			OAuthUriUtil.appendFormUrlEncodedQuery("https://example.com/authorize", params, true));
		assertEquals("openid email", URLDecoder.decode("openid+email", StandardCharsets.UTF_8));
	}

	@Test
	public void formUrlEncodedQueryGoesBeforeAnyFragment() {
		String actual = OAuthUriUtil.appendFormUrlEncodedQuery("https://example.com/authorize#top", Map.of("client_id", "abc"));
		assertEquals("https://example.com/authorize?client_id=abc#top", actual);
	}

	@Test
	public void formUrlEncodedQuerySendsNullValueAsBareName() {
		Map<String, String> params = new LinkedHashMap<>();
		params.put("state", null);
		params.put("client_id", "abc");
		String actual = OAuthUriUtil.appendFormUrlEncodedQuery("https://example.com/authorize", params);
		assertEquals("https://example.com/authorize?state&client_id=abc", actual);
	}

	@Test
	public void literalPlusIsReportedExceptInSpaceDelimitedParameters() {
		String raw = "scope=openid+profile&response_type=code+id_token&client_metadata=%7B%22dc+sd-jwt%22%7D"
			+ "&error_description=End-User+authentication+is+required&login_hint=joe%2Btag%40example.com&state=a+b&state=c+d&ok=x";
		assertEquals(List.of("client_metadata", "state"), OAuthUriUtil.parametersWithLiteralPlus(raw));
	}

	@Test
	public void literalPlusWithMalformedPercentSequenceDoesNotThrow() {
		assertEquals(List.of("a%zz"), OAuthUriUtil.parametersWithLiteralPlus("a%zz=b+c"));
		assertEquals(List.of("state"), OAuthUriUtil.parametersWithLiteralPlus("state=%E0%A4%A+x"));
	}

	@Test
	public void literalPlusInParameterNameIsReported() {
		assertEquals(List.of("a b"), OAuthUriUtil.parametersWithLiteralPlus("a+b=1"));
	}

	@Test
	public void noLiteralPlusGivesEmptyList() {
		assertEquals(List.of(), OAuthUriUtil.parametersWithLiteralPlus("scope=openid%20profile&x=%2B"));
		assertEquals(List.of(), OAuthUriUtil.parametersWithLiteralPlus(""));
		assertEquals(List.of(), OAuthUriUtil.parametersWithLiteralPlus(null));
	}

	@Test
	public void formUrlEncodedQueryAcceptsSchemeOnlyWalletEndpoint() {
		assertEquals("openid4vp://?client_id=abc", OAuthUriUtil.appendFormUrlEncodedQuery("openid4vp://", Map.of("client_id", "abc")));
	}

	@Test
	public void formUrlEncodedQueryRetainsExistingEndpointQuery() {
		String actual = OAuthUriUtil.appendFormUrlEncodedQuery("https://example.com/authorize?tenant=t1", Map.of("client_id", "abc"));
		assertEquals("https://example.com/authorize?tenant=t1&client_id=abc", actual);
	}

	@Test
	public void formUrlEncodedQueryWithNoParametersLeavesUrlUnchanged() {
		assertEquals("https://example.com/authorize", OAuthUriUtil.appendFormUrlEncodedQuery("https://example.com/authorize", Map.of()));
	}

	@Test
	public void wellKnownUrlFromIssuer() {
		String actual = OAuthUriUtil.generateWellKnownUrlForPath("https://iss.acme.test", "oauth-authorization-server");
		String expected = "https://iss.acme.test/.well-known/oauth-authorization-server";
		assertEquals(expected, actual);
	}

	@Test
	public void wellKnownUrlFromIssuerWithSimplePath() {
		String actual = OAuthUriUtil.generateWellKnownUrlForPath("https://iss.acme.test/path", "oauth-authorization-server");
		String expected = "https://iss.acme.test/.well-known/oauth-authorization-server/path";
		assertEquals(expected, actual);
	}

	@Test
	public void wellKnownUrlFromIssuerWithNestedPath() {
		String actual = OAuthUriUtil.generateWellKnownUrlForPath("https://tdworkshops.ngrok.dev/test/a/ssf-tx-oidf", "oauth-authorization-server");
		String expected = "https://tdworkshops.ngrok.dev/.well-known/oauth-authorization-server/test/a/ssf-tx-oidf";
		assertEquals(expected, actual);
	}

	@Test
	public void wellKnownUrlForLocalHostWithPort() {
		String actual = OAuthUriUtil.generateWellKnownUrlForPath("https://localhost.emobix.co.uk:8443/test/a/ssf-tx-oidf", "oauth-authorization-server");
		String expected = "https://localhost.emobix.co.uk:8443/.well-known/oauth-authorization-server/test/a/ssf-tx-oidf";
		assertEquals(expected, actual);
	}

	@Test
	public void wellKnownSsfConfigurationStripsTrailingSlashPerSpec() {
		// OpenID SSF 1.0 sections 7.2 and 7.2.1 require removing any terminating
		// "/" before inserting "/.well-known/ssf-configuration".
		// https://openid.github.io/sharedsignals/openid-sharedsignals-framework-1_0.html#section-7.2.1
		String actual = OAuthUriUtil.generateWellKnownUrlForPath("https://tr.example.com/issuer1/", "ssf-configuration");
		String expected = "https://tr.example.com/.well-known/ssf-configuration/issuer1";
		assertEquals(expected, actual);
	}

	@Test
	public void wellKnownOauthProtectedResourceStripsTrailingSlashPerSpec() {
		// RFC 9728 sections 3 and 3.1 require removing any terminating "/" before
		// inserting "/.well-known/oauth-protected-resource".
		// https://www.rfc-editor.org/rfc/rfc9728.html#section-3.1
		String actual = OAuthUriUtil.generateWellKnownUrlForPath("https://resource.example.com/resource1/", "oauth-protected-resource");
		String expected = "https://resource.example.com/.well-known/oauth-protected-resource/resource1";
		assertEquals(expected, actual);
	}

	@Test
	public void stripTrailingSlashRemovesSingleSlash() {
		assertEquals("/path", OAuthUriUtil.stripTrailingSlash("/path/"));
	}

	@Test
	public void stripTrailingSlashLeavesPathWithoutSlashUntouched() {
		assertEquals("/path", OAuthUriUtil.stripTrailingSlash("/path"));
	}

	@Test
	public void stripTrailingSlashHandlesEmptyAndRootAndNull() {
		assertEquals("", OAuthUriUtil.stripTrailingSlash(""));
		assertEquals("", OAuthUriUtil.stripTrailingSlash("/"));
		assertEquals(null, OAuthUriUtil.stripTrailingSlash(null));
	}

	@Test
	public void stripTrailingSlashOnlyRemovesOneSlash() {
		assertEquals("/path/", OAuthUriUtil.stripTrailingSlash("/path//"));
	}
}
