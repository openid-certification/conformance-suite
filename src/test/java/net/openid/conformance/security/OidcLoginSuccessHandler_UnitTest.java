package net.openid.conformance.security;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;

import java.util.List;

/**
 * Guards the success handler of the OIDC login chain
 * ({@link WebSecurityOidcLoginConfig#oidcLoginSuccessHandler()}).
 *
 * <p>A successful login must evict the browser's cache for the origin
 * ({@code Clear-Site-Data: "cache"}), so copies of static assets cached
 * under an older, longer-lived policy are fetched again, while still
 * returning the user to the page they asked for before logging in.
 */
public class OidcLoginSuccessHandler_UnitTest {

	private static final String BASE = "https://localhost.emobix.co.uk:8443";

	private final Authentication user =
		new UsernamePasswordAuthenticationToken("conformance-user", "N/A", List.of());

	private static MockHttpServletRequest secureRequest(MockHttpSession session) {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/login/oauth2/code/gitlab");
		request.setScheme("https");
		request.setServerName("localhost.emobix.co.uk");
		request.setServerPort(8443);
		request.setSecure(true);
		request.setSession(session);
		return request;
	}

	@Test
	public void login_clears_the_browser_cache() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();

		WebSecurityOidcLoginConfig.oidcLoginSuccessHandler()
			.onAuthenticationSuccess(secureRequest(new MockHttpSession()), response, user);

		Assertions.assertEquals("\"cache\"", response.getHeader("Clear-Site-Data"),
			"login must evict cached static assets so stale long-lived copies are refetched");
	}

	@Test
	public void login_still_returns_to_the_saved_page() throws Exception {
		// What the entry point stores when an anonymous user opens a deep link.
		MockHttpSession session = new MockHttpSession();
		MockHttpServletRequest original = new MockHttpServletRequest("GET", "/plans.html");
		original.setScheme("https");
		original.setServerName("localhost.emobix.co.uk");
		original.setServerPort(8443);
		original.setQueryString("owner=ci");
		original.setSession(session);
		new HttpSessionRequestCache().saveRequest(original, new MockHttpServletResponse());

		MockHttpServletResponse response = new MockHttpServletResponse();
		AuthenticationSuccessHandler handler = WebSecurityOidcLoginConfig.oidcLoginSuccessHandler();
		handler.onAuthenticationSuccess(secureRequest(session), response, user);

		Assertions.assertEquals(HttpServletResponse.SC_FOUND, response.getStatus());
		Assertions.assertEquals(BASE + "/plans.html?owner=ci&continue", response.getRedirectedUrl(),
			"login must keep returning the user to the page they originally requested");
		Assertions.assertEquals("\"cache\"", response.getHeader("Clear-Site-Data"));
	}

	@Test
	public void login_without_a_saved_page_goes_to_the_default_target() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();

		WebSecurityOidcLoginConfig.oidcLoginSuccessHandler()
			.onAuthenticationSuccess(secureRequest(new MockHttpSession()), response, user);

		Assertions.assertEquals("/", response.getRedirectedUrl());
	}
}
