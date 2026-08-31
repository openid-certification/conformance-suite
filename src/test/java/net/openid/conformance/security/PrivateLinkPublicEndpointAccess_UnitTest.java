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
 * Guards the rule ordering in {@link WebSecurityResourceServerConfig}: the {@code ?public=true}
 * permit must be registered BEFORE the private-link deny rule.
 *
 * <p>Regression context: the log-detail page resolves spec-reference chips by fetching
 * {@code GET /api/ui/spec_links?public=true} (see {@code static/lib/spec-links.js}), an endpoint
 * that is world-readable via the public matcher. With the private-link deny rule registered first,
 * a viewer who opened a shared test log via a private link was denied that fetch — the frontend
 * degrades silently to an empty map, so the spec links rendered as dead text. A private-link
 * session must never be MORE restricted than no session at all; anything anonymous viewers can
 * fetch with {@code ?public=true} must stay reachable for private-link viewers too.
 *
 * <p>The deny rule itself must keep doing its job: without {@code ?public=true}, and for paths
 * outside {@code PUBLIC_GET_PATHS}, private-link viewers stay limited to the small read-only
 * allowlist of shared-result endpoints.
 *
 * <p>These tests drive the real {@code filterChainResourceServer} bean (built in a minimal
 * context with mocked collaborators) through a {@link FilterChainProxy}, following the pattern
 * established by {@link ResourceServerRequestCache_UnitTest}. {@link MockFilterChain} stands in
 * for the controller, so a 200 with no body means the request passed the security chain.
 */
public class PrivateLinkPublicEndpointAccess_UnitTest {

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

	private MockHttpServletRequest buildGet(String uri, boolean publicParam, MockHttpSession session) {
		MockHttpServletRequest request = new MockHttpServletRequest(context.getServletContext());
		request.setMethod("GET");
		request.setRequestURI(uri);
		// The chain's RejectPlainHttpTrafficFilter requires https.
		request.setScheme("https");
		request.setSecure(true);
		request.addHeader("Accept", "application/json");
		if (publicParam) {
			// PublicRequestMatcher reads the parameter, not the raw query string.
			request.setQueryString("public=true");
			request.addParameter("public", "true");
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

	/**
	 * A session carrying an authenticated principal, seeded through the session attribute
	 * because {@code SecurityContextHolderFilter} reloads the context from the repository at
	 * the head of the chain.
	 */
	private MockHttpSession sessionWithAuthenticatedUser() {
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SECURITY_CONTEXT_ATTRIBUTE, new SecurityContextImpl(
			UsernamePasswordAuthenticationToken.authenticated("test-user", "n/a",
				AuthorityUtils.createAuthorityList("ROLE_PRIVATE_LINK_USER"))));
		return session;
	}

	private MockHttpSession privateLinkSession() {
		Mockito.when(authenticationFacade.isPrivateLinkUser()).thenReturn(true);
		return sessionWithAuthenticatedUser();
	}

	@Test
	public void private_link_user_can_fetch_public_spec_links() throws Exception {
		MockHttpServletResponse response = run(
			buildGet("/api/ui/spec_links", true, privateLinkSession()));

		Assertions.assertEquals(HttpServletResponse.SC_OK, response.getStatus(),
			"GET /api/ui/spec_links?public=true must pass the chain for a private-link viewer: "
				+ "the shared log-detail page needs it to render spec-reference links, and it is "
				+ "already world-readable anonymously");
	}

	@Test
	public void anonymous_can_fetch_public_spec_links() throws Exception {
		MockHttpServletResponse response = run(buildGet("/api/ui/spec_links", true, null));

		Assertions.assertEquals(HttpServletResponse.SC_OK, response.getStatus(),
			"GET /api/ui/spec_links?public=true must stay anonymously reachable — the "
				+ "private-link test above asserts parity with this baseline");
	}

	@Test
	public void private_link_user_is_denied_spec_links_without_public_param() throws Exception {
		MockHttpServletResponse response = run(
			buildGet("/api/ui/spec_links", false, privateLinkSession()));

		Assertions.assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus(),
			"without ?public=true the private-link deny rule must still apply");
	}

	@Test
	public void public_param_does_not_open_non_public_endpoints_for_private_link_user() throws Exception {
		// /api/lastconfig is not in PUBLIC_GET_PATHS, so ?public=true must not help.
		MockHttpServletResponse response = run(
			buildGet("/api/lastconfig", true, privateLinkSession()));

		Assertions.assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus(),
			"?public=true on a path outside PUBLIC_GET_PATHS must stay denied for "
				+ "private-link viewers");
	}

	@Test
	public void private_link_allowlist_still_reaches_shared_log() throws Exception {
		MockHttpServletResponse response = run(
			buildGet("/api/log/Wu3Ds7vlknQCijL", false, privateLinkSession()));

		Assertions.assertEquals(HttpServletResponse.SC_OK, response.getStatus(),
			"the private-link allowlist for shared-result endpoints must keep working");
	}

	@Configuration
	@EnableWebSecurity
	@Import(WebSecurityResourceServerConfig.class)
	static class TestSecurityConfig {

		@Bean
		public AuthenticationFacade authenticationFacade() {
			// isPrivateLinkUser() defaults to false (Mockito); private-link tests stub it to true.
			return Mockito.mock(AuthenticationFacade.class);
		}

		@Bean
		public DummyUserFilter dummyUserFilter() {
			// Never added to the chain: fintechlabs.devmode defaults to false.
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
