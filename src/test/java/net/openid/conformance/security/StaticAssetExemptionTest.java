package net.openid.conformance.security;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.util.AntPathMatcher;

import java.util.Arrays;

/**
 * Guards the Spring Security static-asset exemption list
 * ({@link WebSecurityConfig#STATIC_RESOURCE_PATTERNS}).
 *
 * Regression context: {@code /lib/**} was missing from the exempt list, so for
 * anonymous users {@code /lib/time-format.js} 302-redirected to {@code /login.html}.
 * The browser then rejected that HTML as an ES module, which cascaded up
 * {@code cts-time.js → cts-plan-list.js / cts-log-list.js} and left the public
 * plans/logs listings rendering nothing for logged-out visitors.
 */
public class StaticAssetExemptionTest {

	private final AntPathMatcher pathMatcher = new AntPathMatcher();

	private boolean isExempt(String path) {
		return Arrays.stream(WebSecurityConfig.STATIC_RESOURCE_PATTERNS)
			.anyMatch(pattern -> pathMatcher.match(pattern, path));
	}

	@Test
	public void lib_module_support_assets_are_exempt() {
		// The exact file whose redirect broke anonymous plans/logs rendering.
		Assertions.assertTrue(isExempt("/lib/time-format.js"),
			"/lib/time-format.js must be exempt so anonymous module loads succeed");
		// Other /lib/ files imported by components that can appear on public pages.
		Assertions.assertTrue(isExempt("/lib/spec-links.js"));
		Assertions.assertTrue(isExempt("/lib/config-field-types.js"));
	}

	@Test
	public void other_public_page_assets_remain_exempt() {
		Assertions.assertTrue(isExempt("/components/cts-time.js"));
		Assertions.assertTrue(isExempt("/js/cts-toast-api.js"));
		Assertions.assertTrue(isExempt("/vendor/lit/lit.js"));
		Assertions.assertTrue(isExempt("/css/oidf-tokens.css"));
	}

	@Test
	public void api_paths_are_not_exempt() {
		// The exemption must never widen to data endpoints — those carry the
		// authentication boundary (WebSecurityResourceServerConfig).
		Assertions.assertFalse(isExempt("/api/plan"),
			"/api/plan must stay inside the security filter chain");
		Assertions.assertFalse(isExempt("/api/plan?public=true"));
		Assertions.assertFalse(isExempt("/api/log"));
		Assertions.assertFalse(isExempt("/api/currentuser"));
	}

	@Test
	public void html_pages_are_not_exempt() {
		// HTML shells stay in the chain (they should revalidate and, for
		// listing pages, are permitted explicitly in WebSecurityOidcLoginConfig).
		Assertions.assertFalse(isExempt("/plans.html"));
		Assertions.assertFalse(isExempt("/logs.html"));
		Assertions.assertFalse(isExempt("/login.html"));
	}
}
