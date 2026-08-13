package net.openid.conformance.security;

import jakarta.servlet.http.HttpServletResponse;
import net.openid.conformance.info.TestInfoService;
import net.openid.conformance.info.TestPlanService;
import net.openid.conformance.sharing.privatelink.PrivateLinkUserDetailsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import net.openid.conformance.security.keycloak.EntitlementsAuthoritiesConverter;
import net.openid.conformance.security.keycloak.IDPLogoutHandler;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.ott.OneTimeTokenService;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import java.util.List;
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
 *
 * Unlike {@link ResourceServerRequestCache_UnitTest} there is deliberately no
 * saved-request assertion here: logout invalidates the session (asserted
 * below), so no {@code SPRING_SECURITY_SAVED_REQUEST} can survive to poison
 * the login flow — do not add one by analogy.
 */
public class OidcLogoutRedirect_UnitTest {

	private static final String IDP_ISSUER_URI = "https://openid.example.com/auth";
	private static final String IDP_END_SESSION_ENDPOINT =
		IDP_ISSUER_URI + "/protocol/openid-connect/logout";
	private static final String BASE_URL = "https://localhost.emobix.co.uk:8443";

	private AnnotationConfigWebApplicationContext context;
	private FilterChainProxy filterChainProxy;

	@BeforeEach
	public void setUp() {
		context = new AnnotationConfigWebApplicationContext();
		context.setServletContext(new MockServletContext());
		// The config has required @Value properties with no defaults; supply
		// the same shapes application.properties uses.
		context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", Map.of(
			"fintechlabs.base_url", "https://localhost.emobix.co.uk:8443")));
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
		// An authenticated session (the real-world shape of a sign-out click)
		// so the test also proves the session-invalidation half of logout.
		MockHttpSession session = new MockHttpSession();
		SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
		securityContext.setAuthentication(
			new UsernamePasswordAuthenticationToken("conformance-user", "N/A", List.of()));
		session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
		request.setSession(session);
		MockHttpServletResponse response = new MockHttpServletResponse();

		filterChainProxy.doFilter(request, response, new MockFilterChain());

		Assertions.assertEquals(HttpServletResponse.SC_FOUND, response.getStatus(),
			"POST /logout must redirect");
		Assertions.assertEquals("/login.html?logout=true", response.getRedirectedUrl(),
			"logout must land on login.html with the ?logout=true banner trigger");
		Assertions.assertEquals("\"cache\"", response.getHeader("Clear-Site-Data"),
			"logout must keep evicting the origin's cache so Back cannot restore an authenticated shell");
		Assertions.assertTrue(session.isInvalid(),
			"logout must invalidate the authenticated session");
	}

	/**
	 * The IdP half of the same contract: a session established through the IdP
	 * must be propagated to the IdP's end-session endpoint, and exactly one
	 * redirect must be written. Wiring the IdP logout as a LogoutHandler instead
	 * of the LogoutSuccessHandler passes the test above while silently losing
	 * this redirect (handlers run first and commit the response), so both halves
	 * need asserting.
	 */
	@Test
	public void logout_of_idp_session_redirects_to_idp_end_session_endpoint() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(context.getServletContext());
		request.setMethod("POST");
		request.setRequestURI("/logout");
		request.setServletPath("/logout");
		request.setScheme("https");
		request.setSecure(true);
		request.setServerName("localhost.emobix.co.uk");
		request.setServerPort(8443);

		OidcIdToken idToken = OidcIdToken.withTokenValue("the-id-token")
			.claim("iss", IDP_ISSUER_URI)
			.claim("sub", "conformance-user")
			.build();
		var authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
		var principal = new DefaultOidcUser(authorities, idToken);
		MockHttpSession session = new MockHttpSession();
		SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
		securityContext.setAuthentication(
			new OAuth2AuthenticationToken(principal, authorities, "idp"));
		session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
		request.setSession(session);
		MockHttpServletResponse response = new MockHttpServletResponse();

		filterChainProxy.doFilter(request, response, new MockFilterChain());

		String redirect = response.getRedirectedUrl();
		Assertions.assertEquals(HttpServletResponse.SC_FOUND, response.getStatus(),
			"POST /logout must redirect");
		Assertions.assertNotNull(redirect, "logout of an IdP session must redirect somewhere");
		Assertions.assertTrue(redirect.startsWith(IDP_END_SESSION_ENDPOINT + "?"),
			"logout must be propagated to the end-session endpoint the IdP advertises"
				+ " (not one derived from the issuer), was: " + redirect);
		Assertions.assertTrue(redirect.contains("id_token_hint=the-id-token"),
			"the IdP needs id_token_hint to honour post_logout_redirect_uri, was: " + redirect);
		Assertions.assertTrue(
			redirect.contains("post_logout_redirect_uri=https://localhost.emobix.co.uk:8443/login.html?logout%3Dtrue")
				|| redirect.contains("post_logout_redirect_uri=https%3A%2F%2Flocalhost.emobix.co.uk%3A8443%2Flogin.html%3Flogout%3Dtrue"),
			"the IdP must send the user back to the ?logout=true banner, was: " + redirect);
		Assertions.assertTrue(session.isInvalid(),
			"logout must invalidate the local session even when propagated to the IdP");
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
			ClientRegistration registration = ClientRegistration.withRegistrationId("idp")
				.clientId("test-client")
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.redirectUri("{baseUrl}/login/oauth2/code/idp")
				.issuerUri(IDP_ISSUER_URI)
				.authorizationUri("https://idp.example.invalid/authorize")
				.tokenUri("https://idp.example.invalid/token")
				// Deliberately NOT issuer + "/logout": Keycloak and friends put the
				// end-session endpoint on a different path, so deriving it from the
				// issuer silently sends users somewhere that does not exist.
				.providerConfigurationMetadata(Map.of("end_session_endpoint", IDP_END_SESSION_ENDPOINT))
				.build();
			return new InMemoryClientRegistrationRepository(registration);
		}

		@Bean
		public IDPLogoutHandler keycloakLogoutHandler(
			org.springframework.beans.factory.ObjectProvider<
				org.springframework.security.oauth2.client.registration.ClientRegistrationRepository> repository) {
			// A @Component in the real app; this minimal context does no
			// component scanning. Built for real, NOT mocked: it owns the logout
			// redirect this test asserts on, so a mock here would make the test
			// pass against a chain that redirects nowhere.
			return new IDPLogoutHandler(repository, BASE_URL);
		}

		@Bean
		public EntitlementsAuthoritiesConverter keyCloakAuthoritiesConverter() {
			// Likewise a scanned @Component; only consulted while mapping
			// authorities for a real IdP login.
			return new EntitlementsAuthoritiesConverter("conformance-admin");
		}

		@Bean
		public MigrationAuthenticationHandler migrationAuthenticationHandler() {
			// A scanned @Component in the real app. Built for real (not mocked)
			// so the chain keeps the SavedRequestAware success-handler behaviour
			// these tests assert on; only its migration collaborators are stubs.
			return new MigrationAuthenticationHandler(
				Mockito.mock(TestPlanService.class), Mockito.mock(TestInfoService.class));
		}
	}
}
