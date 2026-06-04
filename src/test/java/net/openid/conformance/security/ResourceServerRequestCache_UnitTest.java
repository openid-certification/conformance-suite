package net.openid.conformance.security;

import jakarta.servlet.http.HttpServletResponse;
import net.openid.conformance.sharing.privatelink.ShareJwtBearerAuthenticationProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

/**
 * Guards the API filter chain against request-cache poisoning
 * ({@link WebSecurityResourceServerConfig}).
 *
 * Regression context: anonymous {@code fetch()} calls from public pages (the
 * footer's {@code /api/server} version probe on login.html, the navbar's
 * {@code /api/currentuser} probe on the public listing pages) hit
 * authenticated API endpoints and 401. Spring Security's
 * {@code ExceptionTranslationFilter} saved each such request into the
 * session's {@code SPRING_SECURITY_SAVED_REQUEST} attribute BEFORE invoking
 * the 401 entry point. The OIDC login chain shares the same HttpSession, so
 * its default {@code SavedRequestAwareAuthenticationSuccessHandler} replayed
 * the poisoned entry after OAuth login — users landed on raw JSON at
 * {@code /api/server?continue} instead of the plans home.
 *
 * Native {@code fetch()} sends {@code Accept: *}{@code /*} and no
 * {@code X-Requested-With}, which bypasses both AJAX carve-outs in Spring
 * Security's default saved-request matcher. The API chain's
 * {@code SessionCreationPolicy.NEVER} does not help: the default
 * {@code HttpSessionRequestCache} has {@code createSessionAllowed=true} and
 * only {@code STATELESS} (not {@code NEVER}) makes Spring auto-install a
 * {@code NullRequestCache}. The explicit {@code NullRequestCache} on the API
 * chain is therefore load-bearing; this test fails if it is ever removed.
 *
 * These tests drive the real {@code filterChainResourceServer} bean (built in
 * a minimal context with mocked collaborators) through a
 * {@link FilterChainProxy}, mimicking the anonymous browser fetch.
 */
public class ResourceServerRequestCache_UnitTest {

	private static final String SAVED_REQUEST_ATTRIBUTE = "SPRING_SECURITY_SAVED_REQUEST";

	private AnnotationConfigWebApplicationContext context;
	private FilterChainProxy filterChainProxy;

	@BeforeEach
	public void setUp() {
		context = new AnnotationConfigWebApplicationContext();
		context.setServletContext(new MockServletContext());
		context.register(TestSecurityConfig.class);
		context.refresh();
		filterChainProxy = new FilterChainProxy(context.getBean("filterChainResourceServer", SecurityFilterChain.class));
	}

	@AfterEach
	public void tearDown() {
		context.close();
	}

	/**
	 * Sends an anonymous GET through the real API filter chain the way a
	 * browser-native {@code fetch()} would: {@code Accept: *}{@code /*}, no
	 * {@code X-Requested-With}, no Authorization header. The session is
	 * pre-created so the red state is deterministic: without the
	 * {@code NullRequestCache} the saved-request write lands on this session
	 * (rather than on one the cache creates itself), and with the fix the
	 * assertion proves suppression rather than mere absence of a session.
	 */
	private MockHttpServletResponse anonymousFetchStyleGet(String uri, MockHttpSession session) throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(context.getServletContext());
		request.setMethod("GET");
		request.setRequestURI(uri);
		// The chain's RejectPlainHttpTrafficFilter requires https.
		request.setScheme("https");
		request.setSecure(true);
		request.addHeader("Accept", "*/*");
		request.setSession(session);

		MockHttpServletResponse response = new MockHttpServletResponse();
		filterChainProxy.doFilter(request, response, new MockFilterChain());
		return response;
	}

	private void assertUnauthorizedWithoutSavedRequest(String uri) throws Exception {
		MockHttpSession session = new MockHttpSession();

		MockHttpServletResponse response = anonymousFetchStyleGet(uri, session);

		// The authorization behavior is unchanged: a plain 401 from
		// RestAuthenticationEntryPoint, not a redirect to a login flow.
		Assertions.assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus(),
			uri + " must return 401 to anonymous requests");
		Assertions.assertNull(response.getRedirectedUrl(),
			uri + " must not redirect anonymous requests");

		// The invariant under test: the 401 must not leave a saved request
		// behind, or the OIDC chain's success handler will replay this API
		// URL as the post-login redirect target.
		Assertions.assertNull(session.getAttribute(SAVED_REQUEST_ATTRIBUTE),
			uri + " must not write " + SAVED_REQUEST_ATTRIBUTE + " into the session");
	}

	@Test
	public void anonymous_api_server_fetch_is_401_and_leaves_no_saved_request() throws Exception {
		// The exact request whose replay sent users to /api/server?continue.
		assertUnauthorizedWithoutSavedRequest("/api/server");
	}

	@Test
	public void anonymous_api_currentuser_fetch_is_401_and_leaves_no_saved_request() throws Exception {
		// The navbar's auth probe on anonymous plans.html / logs.html visits.
		assertUnauthorizedWithoutSavedRequest("/api/currentuser");
	}

	@Test
	public void protection_is_chain_wide_not_endpoint_specific() throws Exception {
		// An arbitrary authenticated API path proves the request cache is
		// disabled for the whole chain, not for individual endpoints.
		assertUnauthorizedWithoutSavedRequest("/api/runner/some-test-id");
	}

	@Configuration
	@EnableWebSecurity
	@Import(WebSecurityResourceServerConfig.class)
	static class TestSecurityConfig {

		@Bean
		public AuthenticationFacade authenticationFacade() {
			// Mockito default (false) for isPrivateLinkUser() keeps the
			// private-link denyAll matcher out of the way.
			return Mockito.mock(AuthenticationFacade.class);
		}

		@Bean
		public DummyUserFilter dummyUserFilter() {
			// Never added to the chain: fintechlabs.devmode defaults to false,
			// so the anonymous 401 path stays observable.
			return Mockito.mock(DummyUserFilter.class);
		}

		@Bean
		public ApiTokenAuthenticationProvider apiTokenAuthenticationProvider() {
			// Only consulted when an Authorization header is present; the
			// anonymous requests in this test never carry one.
			return Mockito.mock(ApiTokenAuthenticationProvider.class);
		}

		@Bean
		public ShareJwtBearerAuthenticationProvider shareJwtBearerAuthenticationProvider() {
			return Mockito.mock(ShareJwtBearerAuthenticationProvider.class);
		}
	}
}
