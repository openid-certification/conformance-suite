package net.openid.conformance.security;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import java.util.List;

/**
 * Guards the dedicated static-resources filter chain
 * ({@link WebSecurityConfig#filterChainStaticResources}).
 *
 * Migration context: static assets used to be exempted via
 * {@code WebSecurityCustomizer.ignoring()}, which Spring Security warns against
 * ("please use permitAll via HttpSecurity#authorizeHttpRequests instead"). The
 * exemption was replaced by a permitAll() chain. The load-bearing invariant the
 * migration must preserve is the very reason ignoring() was used: Spring
 * Security must NOT stamp its blanket "Cache-Control: no-cache, no-store,
 * max-age=0, must-revalidate" on static assets. Assets without an explicit
 * cache header (e.g. /vendor/**, /images/**) would otherwise revalidate on
 * every navigation and never be stored, defeating the browser cache.
 *
 * These tests drive the real {@code filterChainStaticResources} bean (built in
 * a minimal context) through a {@link FilterChainProxy}.
 */
public class StaticResourcesSecurityChain_UnitTest {

	private AnnotationConfigWebApplicationContext context;
	private FilterChainProxy filterChainProxy;

	@BeforeEach
	public void setUp() {
		context = new AnnotationConfigWebApplicationContext();
		context.setServletContext(new MockServletContext());
		context.register(TestSecurityConfig.class);
		context.refresh();
		filterChainProxy = new FilterChainProxy(
			context.getBean("filterChainStaticResources", SecurityFilterChain.class));
	}

	@AfterEach
	public void tearDown() {
		context.close();
	}

	private MockHttpServletResponse runStaticAssetRequest(String uri) throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(context.getServletContext());
		request.setMethod("GET");
		request.setRequestURI(uri);
		// The chain has no RejectPlainHttpTrafficFilter, but keep requests https
		// so the test reflects production traffic shape behind the TLS proxy.
		request.setScheme("https");
		request.setSecure(true);
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain downstream = new MockFilterChain();
		filterChainProxy.doFilter(request, response, downstream);
		// permitAll → the request reaches the (mock) servlet downstream rather
		// than being short-circuited by a 401 / login redirect.
		Assertions.assertNotNull(downstream.getRequest(),
			uri + " must be permitted through the static-resources chain");
		return response;
	}

	@Test
	public void vendor_asset_is_permitted_without_blanket_no_store() throws Exception {
		MockHttpServletResponse response = runStaticAssetRequest("/vendor/lit/lit.js");
		assertChainProcessedRequest(response, "/vendor/lit/lit.js");
		assertNoBlanketNoStore(response, "/vendor/lit/lit.js");
		Assertions.assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		Assertions.assertNull(response.getRedirectedUrl());
	}

	@Test
	public void images_asset_is_permitted_without_blanket_no_store() throws Exception {
		MockHttpServletResponse response = runStaticAssetRequest("/images/openid.png");
		assertChainProcessedRequest(response, "/images/openid.png");
		assertNoBlanketNoStore(response, "/images/openid.png");
	}

	/**
	 * The static-resources chain must be consulted BEFORE the OIDC login chain
	 * (@Order(2)), whose "everything non-/api/**" matcher also matches these
	 * static paths. Spring orders SecurityFilterChain beans by @Order; this
	 * proves filterChainStaticResources sorts ahead of higher-order chains, so a
	 * regression that drops its @Order would fail here rather than silently
	 * reinstating the no-store headers on static assets in production.
	 */
	@Test
	public void static_chain_is_ordered_before_higher_order_chains() {
		try (AnnotationConfigWebApplicationContext orderingContext = new AnnotationConfigWebApplicationContext()) {
			orderingContext.setServletContext(new MockServletContext());
			orderingContext.register(OrderingTestConfig.class);
			orderingContext.refresh();

			List<SecurityFilterChain> ordered =
				orderingContext.getBeanProvider(SecurityFilterChain.class).orderedStream().toList();
			SecurityFilterChain staticChain =
				orderingContext.getBean("filterChainStaticResources", SecurityFilterChain.class);

			Assertions.assertSame(staticChain, ordered.get(0),
				"the static-resources chain must be ordered ahead of higher-@Order chains");
		}
	}

	/**
	 * Proves the static chain actually engaged for this URI (so the no-store
	 * assertion below cannot pass falsely because the securityMatcher missed the
	 * request and no header writer ran at all). The default HeaderWriterFilter
	 * stamps X-Content-Type-Options: nosniff on every response it processes.
	 */
	private void assertChainProcessedRequest(MockHttpServletResponse response, String uri) {
		Assertions.assertEquals("nosniff", response.getHeader("X-Content-Type-Options"),
			uri + " must be processed by the static-resources security chain");
	}

	private void assertNoBlanketNoStore(MockHttpServletResponse response, String uri) {
		String cacheControl = response.getHeader("Cache-Control");
		Assertions.assertFalse(cacheControl != null && cacheControl.contains("no-store"),
			uri + " must not receive Spring Security's blanket no-store Cache-Control (got: "
				+ cacheControl + ")");
	}

	@Configuration
	@Import(WebSecurityConfig.class)
	static class TestSecurityConfig {
	}

	/**
	 * WebSecurityConfig plus two stub chains at @Order(1)/@Order(2) (standing in
	 * for the API and OIDC chains), each with a specific, non-overlapping matcher
	 * so Spring Security's "any-request chain must be last" rule is not tripped.
	 */
	@Configuration
	@Import(WebSecurityConfig.class)
	static class OrderingTestConfig {

		@Bean
		@Order(1)
		public SecurityFilterChain stubApiChain() {
			return new DefaultSecurityFilterChain(
				PathPatternRequestMatcher.withDefaults().matcher("/stub-api/**"));
		}

		@Bean
		@Order(2)
		public SecurityFilterChain stubOidcChain() {
			return new DefaultSecurityFilterChain(
				PathPatternRequestMatcher.withDefaults().matcher("/stub-oidc/**"));
		}
	}
}
