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
}
