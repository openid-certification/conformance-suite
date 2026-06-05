package net.openid.conformance.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OAuthUriUtilTest {

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
