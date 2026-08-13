package net.openid.conformance.security;

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

	private MockHttpServletRequest buildSecureGet(String path) {
		MockHttpServletRequest request = new MockHttpServletRequest(context.getServletContext());
		request.setMethod("GET");
		request.setRequestURI(path);
		request.setServletPath(path);
		request.setScheme("https");
		request.setSecure(true);
		request.setServerName("localhost.emobix.co.uk");
		request.setServerPort(8443);
		request.addHeader("Accept", "text/html");
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
