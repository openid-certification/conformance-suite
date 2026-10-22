package net.openid.conformance.security;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletResponse;
import net.openid.conformance.info.TestPlanService;
import net.openid.conformance.sharing.privatelink.PrivateLinkUserDetailsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.ott.OneTimeTokenService;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import java.util.List;
import java.util.Map;

/**
 * Guards the listing-page access contract in {@link WebSecurityOidcLoginConfig}.
 *
 * Bare listing-page URLs are authenticated "My" views. Anonymous users should
 * be redirected to login through Spring Security so the original URL is saved
 * and replayed after authentication. Public browsing is still anonymous, but
 * only through the explicit {@code ?public=true} URLs used by the navbar and
 * login page.
 */
public class OidcListingPageAccess_UnitTest {

	private AnnotationConfigWebApplicationContext context;
	private FilterChainProxy filterChainProxy;

	@BeforeEach
	public void setUp() {
		context = new AnnotationConfigWebApplicationContext();
		context.setServletContext(new MockServletContext());
		context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", Map.of(
			"fintechlabs.base_url", "https://localhost.emobix.co.uk:8443",
			"oidc.admin.issuer", "https://gitlab.com",
			"oidc.gitlab.admin-group-indicator-claims",
			"{'https://gitlab.org/claims/groups/maintainer':{'openid'}}")));
		context.register(TestSecurityConfig.class);
		context.refresh();
		filterChainProxy = new FilterChainProxy(context.getBean("filterChainOidc", SecurityFilterChain.class));
	}

	@AfterEach
	public void tearDown() {
		context.close();
	}

	/** A browser navigating to {@code path}: it asks for HTML. */
	private MockHttpServletRequest buildSecureGet(String path) {
		return buildSecureGet(path, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
	}

	/**
	 * @param accept the Accept header, or null to send none, as a wallet or a script might
	 */
	private MockHttpServletRequest buildSecureGet(String path, String accept) {
		MockHttpServletRequest request = new MockHttpServletRequest(context.getServletContext());
		request.setMethod("GET");
		request.setRequestURI(path);
		request.setServletPath(path);
		request.setScheme("https");
		request.setSecure(true);
		request.setServerName("localhost.emobix.co.uk");
		request.setServerPort(8443);
		if (accept != null) {
			request.addHeader("Accept", accept);
		}
		return request;
	}

	private void addAuthenticatedSession(MockHttpServletRequest request) {
		MockHttpSession session = new MockHttpSession();
		SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
		securityContext.setAuthentication(
			new UsernamePasswordAuthenticationToken("conformance-user", "N/A", List.of()));
		session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
		request.setSession(session);
	}

	private void assertAnonymousBareListingRedirectsToLoginAndSavesRequest(String path) throws Exception {
		MockHttpServletRequest request = buildSecureGet(path);
		MockHttpServletResponse response = new MockHttpServletResponse();

		filterChainProxy.doFilter(request, response, new MockFilterChain());

		Assertions.assertEquals(HttpServletResponse.SC_FOUND, response.getStatus(),
			path + " must redirect anonymous users to the login flow");
		Assertions.assertEquals("https://localhost.emobix.co.uk:8443/login.html", response.getRedirectedUrl(),
			path + " must use the configured login page");

		SavedRequest savedRequest = new HttpSessionRequestCache().getRequest(request, response);
		Assertions.assertNotNull(savedRequest, path + " must be saved for post-login replay");
		Assertions.assertEquals("https://localhost.emobix.co.uk:8443" + path + "?continue", savedRequest.getRedirectUrl(),
			path + " must replay the original bare listing URL through Spring Security's saved-request flow");
	}

	private void assertAnonymousPublicListingIsPermitted(String path) throws Exception {
		MockHttpServletRequest request = buildSecureGet(path);
		request.setQueryString("public=true");
		request.addParameter("public", "true");
		MockHttpServletResponse response = new MockHttpServletResponse();

		filterChainProxy.doFilter(request, response, new MockFilterChain());

		Assertions.assertEquals(HttpServletResponse.SC_OK, response.getStatus(),
			path + "?public=true must be allowed through to the static page");
		Assertions.assertNull(response.getRedirectedUrl(),
			path + "?public=true must not enter the login flow");
		Assertions.assertNull(new HttpSessionRequestCache().getRequest(request, response),
			path + "?public=true must not create a saved request");
	}

	private void assertAuthenticatedBareListingIsPermitted(String path) throws Exception {
		MockHttpServletRequest request = buildSecureGet(path);
		addAuthenticatedSession(request);
		MockHttpServletResponse response = new MockHttpServletResponse();

		filterChainProxy.doFilter(request, response, new MockFilterChain());

		Assertions.assertEquals(HttpServletResponse.SC_OK, response.getStatus(),
			path + " must be allowed for authenticated users");
		Assertions.assertNull(response.getRedirectedUrl(),
			path + " must not redirect authenticated users");
	}

	@Test
	public void anonymous_bare_plans_page_redirects_to_login_and_saves_original_url() throws Exception {
		assertAnonymousBareListingRedirectsToLoginAndSavesRequest("/plans.html");
	}

	@Test
	public void anonymous_bare_logs_page_redirects_to_login_and_saves_original_url() throws Exception {
		assertAnonymousBareListingRedirectsToLoginAndSavesRequest("/logs.html");
	}

	private void assertSavedPageIsStillPlans(MockHttpServletRequest request, MockHttpServletResponse response, String reason) {
		SavedRequest savedRequest = new HttpSessionRequestCache().getRequest(request, response);
		Assertions.assertNotNull(savedRequest, reason);
		Assertions.assertEquals("https://localhost.emobix.co.uk:8443/plans.html?owner=ci&to=2025-01-01&continue",
			savedRequest.getRedirectUrl(), reason);
	}

	private void assertAnonymousIconProbeKeepsSavedPage(String iconPath) throws Exception {
		MockHttpServletRequest pageRequest = buildSecureGet("/plans.html");
		pageRequest.setQueryString("owner=ci&to=2025-01-01");
		filterChainProxy.doFilter(pageRequest, new MockHttpServletResponse(), new MockFilterChain());

		MockHttpServletRequest iconRequest = buildSecureGet(iconPath);
		iconRequest.setSession(pageRequest.getSession());
		MockHttpServletResponse iconResponse = new MockHttpServletResponse();
		filterChainProxy.doFilter(iconRequest, iconResponse, new MockFilterChain());

		Assertions.assertNull(iconResponse.getRedirectedUrl(), iconPath + " must not enter the login flow");
		assertSavedPageIsStillPlans(iconRequest, iconResponse,
			iconPath + " must not replace the page saved for post-login replay");
	}

	@Test
	public void anonymous_browser_icon_probes_do_not_replace_saved_page() throws Exception {
		// Safari requests these on its own after loading login.html; iOS also tries size-specific names.
		assertAnonymousIconProbeKeepsSavedPage("/apple-touch-icon.png");
		assertAnonymousIconProbeKeepsSavedPage("/apple-touch-icon-precomposed.png");
		assertAnonymousIconProbeKeepsSavedPage("/apple-touch-icon-120x120-precomposed.png");
	}

	private void assertAnonymousNonBrowserRequestIs404(String path, String accept) throws Exception {
		MockHttpServletRequest request = buildSecureGet(path, accept);
		MockHttpServletResponse response = new MockHttpServletResponse();

		filterChainProxy.doFilter(request, response, new MockFilterChain());

		String description = path + " with Accept " + accept;
		Assertions.assertNull(response.getRedirectedUrl(), description + " must not enter the login flow");
		Assertions.assertEquals(HttpServletResponse.SC_NOT_FOUND, response.getStatus(),
			description + " is not a browser navigation, so it must get a 404 rather than a login redirect");
		Assertions.assertNull(new HttpSessionRequestCache().getRequest(request, response),
			description + " must not create a saved request");
	}

	@Test
	public void anonymous_non_browser_request_gets_404_instead_of_login_redirect() throws Exception {
		// a wallet or script asking for an unknown path, or for a page, cannot log in: a redirect
		// would only suggest the path exists but needs a session (issue #1817)
		assertAnonymousNonBrowserRequestIs404("/nonexistent.html", "application/json");
		assertAnonymousNonBrowserRequestIs404("/some/random/path", "*/*");
		assertAnonymousNonBrowserRequestIs404("/plans.html.bak", null);
		assertAnonymousNonBrowserRequestIs404("/plans.html", "application/json");
		assertAnonymousNonBrowserRequestIs404("/api-document.html", "*/*");
	}

	@Test
	public void anonymous_browser_navigation_to_unknown_path_still_goes_to_login() throws Exception {
		// a browser can log in, so an unknown path is treated like any page: login first
		assertAnonymousBareListingRedirectsToLoginAndSavesRequest("/nonexistent.html");
	}

	@Test
	public void anonymous_non_browser_request_does_not_replace_saved_page() throws Exception {
		MockHttpServletRequest pageRequest = buildSecureGet("/plans.html");
		pageRequest.setQueryString("owner=ci&to=2025-01-01");
		filterChainProxy.doFilter(pageRequest, new MockHttpServletResponse(), new MockFilterChain());

		MockHttpServletRequest unknownRequest = buildSecureGet("/nonexistent.html", "application/json");
		unknownRequest.setSession(pageRequest.getSession());
		MockHttpServletResponse unknownResponse = new MockHttpServletResponse();
		filterChainProxy.doFilter(unknownRequest, unknownResponse, new MockFilterChain());

		Assertions.assertEquals(HttpServletResponse.SC_NOT_FOUND, unknownResponse.getStatus());
		assertSavedPageIsStillPlans(unknownRequest, unknownResponse,
			"a request answered with 404 must not replace the page saved for post-login replay");
	}

	@Test
	public void anonymous_browser_navigation_to_any_page_redirects_to_login_and_saves_original_url() throws Exception {
		// a page that is not one of the listing pages
		assertAnonymousBareListingRedirectsToLoginAndSavesRequest("/schedule-test.html");
		// the Swagger UI, which springdoc serves from its own jar rather than from the static directory
		assertAnonymousBareListingRedirectsToLoginAndSavesRequest("/api-document.html");
	}

	@Test
	public void anonymous_error_dispatch_does_not_replace_saved_page() throws Exception {
		// A permitted path that has no file 404s, and the container re-dispatches
		// the request to /error. That dispatch must not enter the login flow.
		MockHttpServletRequest pageRequest = buildSecureGet("/plans.html");
		pageRequest.setQueryString("owner=ci&to=2025-01-01");
		filterChainProxy.doFilter(pageRequest, new MockHttpServletResponse(), new MockFilterChain());

		MockHttpServletRequest errorRequest = buildSecureGet("/error");
		errorRequest.setDispatcherType(DispatcherType.ERROR);
		errorRequest.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, HttpServletResponse.SC_NOT_FOUND);
		errorRequest.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, "/apple-touch-icon-120x120-precomposed.png");
		errorRequest.setSession(pageRequest.getSession());
		MockHttpServletResponse errorResponse = new MockHttpServletResponse();
		filterChainProxy.doFilter(errorRequest, errorResponse, new MockFilterChain());

		Assertions.assertNull(errorResponse.getRedirectedUrl(), "/error dispatch must not enter the login flow");
		assertSavedPageIsStillPlans(errorRequest, errorResponse,
			"/error dispatch must not replace the page saved for post-login replay");
	}

	@Test
	public void anonymous_public_listing_pages_remain_permitted() throws Exception {
		assertAnonymousPublicListingIsPermitted("/plans.html");
		assertAnonymousPublicListingIsPermitted("/logs.html");
	}

	@Test
	public void authenticated_bare_listing_pages_remain_permitted() throws Exception {
		assertAuthenticatedBareListingIsPermitted("/plans.html");
		assertAuthenticatedBareListingIsPermitted("/logs.html");
	}

	@Test
	public void anonymous_misplaced_well_known_path_is_not_sent_to_login() throws Exception {
		// A wallet that forgets the "/.well-known/" prefix (issue #1817) must get the hint
		// controller's 404, not a redirect that looks like the metadata needs a login.
		assertAnonymousMisplacedWellKnownPathIsPermitted("/oauth-authorization-server/test/a/my-wallet/");
		assertAnonymousMisplacedWellKnownPathIsPermitted("/openid-credential-issuer/test/a/my-wallet");
		assertAnonymousMisplacedWellKnownPathIsPermitted("/openid-configuration");
	}

	private void assertAnonymousMisplacedWellKnownPathIsPermitted(String path) throws Exception {
		MockHttpServletRequest request = buildSecureGet(path, "application/json");
		MockHttpServletResponse response = new MockHttpServletResponse();

		filterChainProxy.doFilter(request, response, new MockFilterChain());

		Assertions.assertNull(response.getRedirectedUrl(),
			path + " must not enter the login flow");
		Assertions.assertEquals(HttpServletResponse.SC_OK, response.getStatus(),
			path + " must be allowed through to the hint controller");
		Assertions.assertNull(new HttpSessionRequestCache().getRequest(request, response),
			path + " must not create a saved request");
	}

	@Configuration
	@EnableWebSecurity
	@Import(WebSecurityOidcLoginConfig.class)
	static class TestSecurityConfig {

		@Bean
		public AuthenticationFacade authenticationFacade() {
			return Mockito.mock(AuthenticationFacade.class);
		}

		@Bean
		public DummyUserFilter dummyUserFilter() {
			return Mockito.mock(DummyUserFilter.class);
		}

		@Bean
		public TestPlanService testPlanService() {
			return Mockito.mock(TestPlanService.class);
		}

		@Bean
		public PrivateLinkUserDetailsService privateLinkUserDetailsService() {
			return Mockito.mock(PrivateLinkUserDetailsService.class);
		}

		@Bean
		public OneTimeTokenService oneTimeTokenService() {
			return Mockito.mock(OneTimeTokenService.class);
		}

		@Bean
		public InMemoryClientRegistrationRepository clientRegistrationRepository() {
			ClientRegistration registration = ClientRegistration.withRegistrationId("test")
				.clientId("test-client")
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.redirectUri("{baseUrl}/login/oauth2/code/test")
				.authorizationUri("https://idp.example.invalid/authorize")
				.tokenUri("https://idp.example.invalid/token")
				.build();
			return new InMemoryClientRegistrationRepository(registration);
		}
	}
}
