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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

/**
 * Guards the authentication gate on {@code /api/favorite-plans}
 * ({@link WebSecurityResourceServerConfig}).
 *
 * <p>Favorites are account data keyed on the authenticated principal, so every method on the
 * endpoint must be unreachable without one. The endpoint is listed in that config's API matcher
 * as {@code authenticated()}; this test pins the actual runtime outcome rather than the
 * configuration's shape, so a future refactor that moves the endpoint into the public matcher, or
 * drops it from the matcher entirely (where {@code anyRequest().denyAll()} would still deny it but
 * a later allowlist edit might not), fails here.
 *
 * <p>The second half of the test pins the private-link coupling. A private-link viewer is a real
 * authenticated principal from the filter chain's point of view — and
 * {@code OIDCAuthenticationFacade.getPrincipal()} returns the SHARED ASSET OWNER for them, not the
 * viewer. Favorites are safe only because the private-link rule in
 * {@link WebSecurityResourceServerConfig} denies everything outside its small read-only allowlist,
 * so the favorites endpoints never reach the service. If they were ever added to that allowlist, a
 * private-link viewer could read and mutate the sharer's favorites.
 *
 * <p>These tests drive the real {@code filterChainResourceServer} bean (built in a minimal context
 * with mocked collaborators) through a {@link FilterChainProxy}, following the pattern established
 * by {@link ResourceServerRequestCache_UnitTest}.
 */
public class FavoritePlansEndpointSecurity_UnitTest {

	private static final String SECURITY_CONTEXT_ATTRIBUTE = "SPRING_SECURITY_CONTEXT";

	private AnnotationConfigWebApplicationContext context;
	private FilterChainProxy filterChainProxy;
	private AuthenticationFacade authenticationFacade;

	@BeforeEach
	public void setUp() {
		context = new AnnotationConfigWebApplicationContext();
		context.setServletContext(new MockServletContext());
		context.register(TestSecurityConfig.class);
		context.refresh();
		filterChainProxy = new FilterChainProxy(
			context.getBean("filterChainResourceServer", SecurityFilterChain.class));
		authenticationFacade = context.getBean(AuthenticationFacade.class);
	}

	@AfterEach
	public void tearDown() {
		context.close();
	}

	private MockHttpServletRequest buildRequest(String method, String uri, MockHttpSession session) {
		MockHttpServletRequest request = new MockHttpServletRequest(context.getServletContext());
		request.setMethod(method);
		request.setRequestURI(uri);
		// The chain's RejectPlainHttpTrafficFilter requires https.
		request.setScheme("https");
		request.setSecure(true);
		request.addHeader("Accept", "application/json");
		if (session != null) {
			request.setSession(session);
		}
		return request;
	}

	private MockHttpServletResponse run(MockHttpServletRequest request) throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();
		filterChainProxy.doFilter(request, response, new MockFilterChain());
		return response;
	}

	private void assertAnonymousIsUnauthorized(String method, String uri) throws Exception {
		MockHttpServletResponse response = run(buildRequest(method, uri, null));

		Assertions.assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus(),
			method + " " + uri + " must return 401 to anonymous requests");
		Assertions.assertNull(response.getRedirectedUrl(),
			method + " " + uri + " must not redirect anonymous requests");
	}

	@Test
	public void anonymous_get_favorite_plans_is_unauthorized() throws Exception {
		assertAnonymousIsUnauthorized("GET", "/api/favorite-plans");
	}

	@Test
	public void anonymous_post_favorite_plans_is_unauthorized() throws Exception {
		assertAnonymousIsUnauthorized("POST", "/api/favorite-plans");
	}

	@Test
	public void anonymous_delete_favorite_plan_is_unauthorized() throws Exception {
		assertAnonymousIsUnauthorized("DELETE", "/api/favorite-plans/some-plan-name");
	}

	/**
	 * A logged-in user reaches the endpoint — the 401s above are the auth gate doing its job, not
	 * the endpoint being unreachable for everyone. {@link MockFilterChain} stands in for the
	 * controller, so a 200 with no body means the request made it through the security chain.
	 */
	@Test
	public void authenticated_get_favorite_plans_passes_the_chain() throws Exception {
		MockHttpServletResponse response = run(buildRequest("GET", "/api/favorite-plans",
			sessionWithAuthenticatedUser()));

		Assertions.assertEquals(HttpServletResponse.SC_OK, response.getStatus(),
			"an authenticated GET must pass the security chain to the controller");
	}

	private void assertPrivateLinkUserIsDenied(String method, String uri) throws Exception {
		Mockito.when(authenticationFacade.isPrivateLinkUser()).thenReturn(true);

		MockHttpServletResponse response = run(buildRequest(method, uri,
			sessionWithAuthenticatedUser()));

		Assertions.assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus(),
			method + " " + uri + " must be denied to private-link viewers: getPrincipal() "
				+ "resolves to the SHARED ASSET OWNER for them, so reaching the service would "
				+ "expose the sharer's favorites");
	}

	@Test
	public void private_link_user_cannot_read_favorite_plans() throws Exception {
		assertPrivateLinkUserIsDenied("GET", "/api/favorite-plans");
	}

	@Test
	public void private_link_user_cannot_add_a_favorite_plan() throws Exception {
		assertPrivateLinkUserIsDenied("POST", "/api/favorite-plans");
	}

	@Test
	public void private_link_user_cannot_remove_a_favorite_plan() throws Exception {
		assertPrivateLinkUserIsDenied("DELETE", "/api/favorite-plans/some-plan-name");
	}

	/**
	 * A session carrying an authenticated principal. Seeded through the session attribute rather
	 * than {@code SecurityContextHolder} because {@code SecurityContextHolderFilter} reloads the
	 * context from the repository at the head of the chain and would discard a thread-local one.
	 */
	private MockHttpSession sessionWithAuthenticatedUser() {
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SECURITY_CONTEXT_ATTRIBUTE, new SecurityContextImpl(
			UsernamePasswordAuthenticationToken.authenticated("test-user", "n/a",
				AuthorityUtils.createAuthorityList("ROLE_USER"))));
		return session;
	}

	@Configuration
	@EnableWebSecurity
	@Import(WebSecurityResourceServerConfig.class)
	static class TestSecurityConfig {

		@Bean
		public AuthenticationFacade authenticationFacade() {
			// isPrivateLinkUser() defaults to false (Mockito), which is the ordinary logged-in
			// user; the private-link tests stub it to true.
			return Mockito.mock(AuthenticationFacade.class);
		}

		@Bean
		public DummyUserFilter dummyUserFilter() {
			// Never added to the chain: fintechlabs.devmode defaults to false, so the anonymous
			// 401 path stays observable.
			return Mockito.mock(DummyUserFilter.class);
		}

		@Bean
		public ApiTokenAuthenticationProvider apiTokenAuthenticationProvider() {
			return Mockito.mock(ApiTokenAuthenticationProvider.class);
		}

		@Bean
		public ShareJwtBearerAuthenticationProvider shareJwtBearerAuthenticationProvider() {
			return Mockito.mock(ShareJwtBearerAuthenticationProvider.class);
		}

		@Bean
		public JwtAuthenticationProvider idpJwtAuthenticationProvider() {
			// Overrides the config's bean, which needs a RolesAuthoritiesConverter and
			// would reach the IdP as soon as a test sent an Authorization header. Like
			// the other providers, it is only consulted when one is present, and these
			// tests never send one. Mirrors ResourceServerRequestCache_UnitTest.
			return Mockito.mock(JwtAuthenticationProvider.class);
		}
	}
}
