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
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

/**
 * Guards the security chain in front of the bulk plan delete - {@code DELETE /api/plan} and the
 * three endpoints that go with it ({@link WebSecurityResourceServerConfig}).
 *
 * <p>These endpoints delete other people's data in bulk, so the two things worth pinning are that
 * nobody unauthenticated reaches them, and that a <b>private-link viewer</b> never does either.
 * That second one is not obvious from the configuration: a private-link viewer is a real
 * authenticated principal as far as the chain is concerned, and
 * {@code OIDCAuthenticationFacade.getPrincipal()} resolves to the SHARED ASSET OWNER for them.
 * They are kept out only because the private-link rule denies everything outside a small
 * read-only allowlist, and only because the paths here fall outside it: {@code /api/plan} has no
 * id segment, and {@code delete-status} / {@code delete-preview} contain a hyphen, which
 * {@code /api/plan/[A-Za-z0-9]+} does not match. Either could change without anyone noticing,
 * which is what this test is for.
 *
 * <p>Admin-only is enforced in the controller rather than in the chain (the {@code TokenApi}
 * precedent), so the last test here pins what the chain does with an ordinary logged-in user -
 * lets them through to the controller, which is where the 403 comes from. That the 403 really
 * happens is asserted at runtime by {@code scripts/run-security-tests.py}, which cannot assert
 * the admin path at all: an API token never carries ROLE_ADMIN.
 *
 * <p>Follows {@link FavoritePlansEndpointSecurity_UnitTest}, driving the real
 * {@code filterChainResourceServer} bean through a {@link FilterChainProxy}.
 */
public class BulkPlanDeleteSecurity_UnitTest {

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
		return buildRequest(method, uri, session, false);
	}

	private MockHttpServletRequest buildRequest(String method, String uri, MockHttpSession session,
												boolean asPublicRequest) {
		MockHttpServletRequest request = new MockHttpServletRequest(context.getServletContext());
		request.setMethod(method);
		request.setRequestURI(uri);
		// The chain's RejectPlainHttpTrafficFilter requires https.
		request.setScheme("https");
		request.setSecure(true);
		request.addHeader("Accept", "application/json");
		if (asPublicRequest) {
			request.setParameter("public", "true");
		}
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
	public void anonymous_bulk_delete_is_unauthorized() throws Exception {
		assertAnonymousIsUnauthorized("DELETE", "/api/plan");
	}

	@Test
	public void anonymous_bulk_delete_preview_is_unauthorized() throws Exception {
		assertAnonymousIsUnauthorized("GET", "/api/plan/delete-preview");
	}

	@Test
	public void anonymous_bulk_delete_status_is_unauthorized() throws Exception {
		assertAnonymousIsUnauthorized("GET", "/api/plan/delete-status");
	}

	@Test
	public void anonymous_bulk_delete_cancel_is_unauthorized() throws Exception {
		assertAnonymousIsUnauthorized("POST", "/api/plan/delete-cancel");
	}

	private void assertPrivateLinkUserIsDenied(String method, String uri) throws Exception {
		Mockito.when(authenticationFacade.isPrivateLinkUser()).thenReturn(true);

		MockHttpServletResponse response = run(buildRequest(method, uri, sessionWithAuthenticatedUser()));

		Assertions.assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus(),
			method + " " + uri + " must be denied to private-link viewers: getPrincipal() resolves "
				+ "to the SHARED ASSET OWNER for them, so reaching the controller would let a "
				+ "viewer delete the sharer's plans");
	}

	@Test
	public void private_link_user_cannot_bulk_delete() throws Exception {
		assertPrivateLinkUserIsDenied("DELETE", "/api/plan");
	}

	@Test
	public void private_link_user_cannot_preview_a_bulk_delete() throws Exception {
		assertPrivateLinkUserIsDenied("GET", "/api/plan/delete-preview");
	}

	@Test
	public void private_link_user_cannot_read_the_bulk_delete_status() throws Exception {
		assertPrivateLinkUserIsDenied("GET", "/api/plan/delete-status");
	}

	@Test
	public void private_link_user_cannot_cancel_a_bulk_delete() throws Exception {
		assertPrivateLinkUserIsDenied("POST", "/api/plan/delete-cancel");
	}

	/**
	 * {@code ?public=true} is a way <b>past</b> this chain, and the reason the controller's own
	 * {@code isAdmin()} check is load bearing rather than belt and braces. {@code
	 * getPublicMatcher} permits {@code GET /api/plan/?*} on a public request, and {@code ?*} is
	 * any one path segment - which includes {@code delete-preview} and {@code delete-status},
	 * a pattern written for {@code /api/plan/{id}}. So an anonymous caller who adds that one
	 * parameter passes the chain, and only {@code TestPlanApi} refuses them.
	 *
	 * <p>Pinned here because it is the opposite of what the 401 tests above suggest, and
	 * because the 403 it relies on is proved at runtime rather than here - see
	 * {@code scripts/run-security-tests.py}, "rejected as a public request too". Only the two
	 * GETs are affected: the matcher is GET-only, so {@code DELETE /api/plan} and
	 * {@code POST /api/plan/delete-cancel} stay behind the chain whatever is asked for.
	 */
	@Test
	public void a_public_request_reaches_the_controller_where_admin_is_checked() throws Exception {
		for (String uri : new String[] { "/api/plan/delete-preview", "/api/plan/delete-status" }) {
			MockHttpServletResponse response = run(buildRequest("GET", uri, null, true));

			Assertions.assertEquals(HttpServletResponse.SC_OK, response.getStatus(),
				"GET " + uri + "?public=true passes the security chain, so TestPlanApi's isAdmin() "
					+ "check is the only thing refusing an anonymous caller - do not remove it");
		}
	}

	@Test
	public void a_public_request_does_not_reach_the_delete_or_the_cancel() throws Exception {
		// the public matcher is GET-only, so these two never leave the chain
		assertAnonymousIsUnauthorized("DELETE", "/api/plan");

		MockHttpServletResponse response = run(buildRequest("POST", "/api/plan/delete-cancel", null, true));

		Assertions.assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus(),
			"POST /api/plan/delete-cancel?public=true must still be refused by the chain");
	}

	/**
	 * An ordinary logged-in user reaches the controller, which is where admin-only is decided.
	 * {@link MockFilterChain} stands in for it, so a 200 with no body means the request passed the
	 * security chain - the 401s above are the auth gate working, not the endpoint being
	 * unreachable for everyone.
	 */
	@Test
	public void an_authenticated_user_reaches_the_controller_where_admin_is_checked() throws Exception {
		MockHttpServletResponse response = run(buildRequest("DELETE", "/api/plan",
			sessionWithAuthenticatedUser()));

		Assertions.assertEquals(HttpServletResponse.SC_OK, response.getStatus(),
			"an authenticated DELETE must pass the security chain to the controller, which is "
				+ "what answers 403 to anyone who is not an admin");
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
	}
}
