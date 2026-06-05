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
import org.springframework.security.authentication.ott.OneTimeTokenService;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

import java.util.Map;

/**
 * Guards the logout contract of the OIDC login chain
 * ({@link WebSecurityOidcLoginConfig}).
 *
 * The logout redirect must carry {@code ?logout=true} because login.html only
 * shows its "You have been logged out." confirmation banner when the
 * {@code logout} URL parameter has a truthy value — a bare {@code /login.html}
 * redirect (the pre-fix behavior) leaves the user on a silent login page with
 * no confirmation that sign-out worked.
 *
 * The logout response must also keep the {@code Clear-Site-Data: "cache"}
 * header: pages are bfcache-eligible (Cache-Control: no-cache), and this
 * header is what evicts the origin's cached/bfcached authenticated shell so
 * the Back button cannot restore it on a shared machine.
 *
 * Mirrors the {@link ResourceServerRequestCache_UnitTest} approach: drive the
 * real {@code filterChainOidc} bean (built in a minimal context with mocked
 * collaborators) through a {@link FilterChainProxy} and assert on the
 * response through Spring's public servlet API.
 */
public class OidcLogoutRedirect_UnitTest {

	private AnnotationConfigWebApplicationContext context;
	private FilterChainProxy filterChainProxy;

	@BeforeEach
	public void setUp() {
		context = new AnnotationConfigWebApplicationContext();
		context.setServletContext(new MockServletContext());
		// The config has required @Value properties with no defaults; supply
		// the same shapes application.properties uses.
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

	@Test
	public void logout_redirects_to_login_with_logout_param_and_clears_site_data() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(context.getServletContext());
		request.setMethod("POST");
		request.setRequestURI("/logout");
		// The logout matcher tests servletPath + pathInfo, which
		// MockHttpServletRequest leaves empty by default — without this the
		// request falls through to the authorization filter instead.
		request.setServletPath("/logout");
		// The chain's RejectPlainHttpTrafficFilter requires https, and the
		// ClearSiteDataHeaderWriter only writes on secure requests.
		request.setScheme("https");
		request.setSecure(true);
		request.setSession(new MockHttpSession());
		MockHttpServletResponse response = new MockHttpServletResponse();

		filterChainProxy.doFilter(request, response, new MockFilterChain());

		Assertions.assertEquals(HttpServletResponse.SC_FOUND, response.getStatus(),
			"POST /logout must redirect");
		Assertions.assertEquals("/login.html?logout=true", response.getRedirectedUrl(),
			"logout must land on login.html with the ?logout=true banner trigger");
		Assertions.assertEquals("\"cache\"", response.getHeader("Clear-Site-Data"),
			"logout must keep evicting the origin's cache so Back cannot restore an authenticated shell");
	}

	@Configuration
	@EnableWebSecurity
	@Import(WebSecurityOidcLoginConfig.class)
	static class TestSecurityConfig {

		@Bean
		public AuthenticationFacade authenticationFacade() {
			// Mockito default (false) for isPrivateLinkUser() keeps the
			// private-link denyAll matcher out of the way.
			return Mockito.mock(AuthenticationFacade.class);
		}

		@Bean
		public DummyUserFilter dummyUserFilter() {
			// Never added to the chain: fintechlabs.devmode defaults to false.
			return Mockito.mock(DummyUserFilter.class);
		}

		@Bean
		public TestPlanService testPlanService() {
			// Only consulted by the private-link matcher, which stays inert.
			return Mockito.mock(TestPlanService.class);
		}

		@Bean
		public PrivateLinkUserDetailsService privateLinkUserDetailsService() {
			return Mockito.mock(PrivateLinkUserDetailsService.class);
		}

		@Bean(name = "mvcHandlerMappingIntrospector")
		public HandlerMappingIntrospector mvcHandlerMappingIntrospector() {
			// The chain's string-pattern matchers resolve to MvcRequestMatcher
			// because Spring MVC is on the classpath; Spring Security insists
			// on this bean being present in the same context.
			return new HandlerMappingIntrospector();
		}

		@Bean
		public OneTimeTokenService oneTimeTokenService() {
			// Overrides the config's CustomOneTimeTokenService bean, whose
			// AssetSharing collaborator drags in KeyManager and friends via
			// field injection — none of which the logout path touches.
			return Mockito.mock(OneTimeTokenService.class);
		}

		@Bean
		public InMemoryClientRegistrationRepository clientRegistrationRepository() {
			// Overrides the config's properties-driven bean: an empty
			// OAuth2ClientProperties would make InMemoryClientRegistrationRepository
			// reject its empty registration list at refresh time. One synthetic
			// registration keeps oauth2Login/oauth2Client wiring satisfied;
			// nothing in the logout path ever resolves it.
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
