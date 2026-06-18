package net.openid.conformance;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.util.AntPathMatcher;

import java.util.Arrays;

/**
 * Guards the cache policy split in {@link ApplicationConfig}:
 *
 * - Production pages send "no-cache" (revalidate every load, but
 *   bfcache-eligible so Back/Forward restores instantly) instead of the
 *   Spring Security blanket "no-store" that blocked the back/forward cache
 *   entirely. The logout-side companion is Clear-Site-Data: "cache"
 *   (WebSecurityOidcLoginConfig).
 * - Production hot asset dirs (/css, /js, /components) get bounded
 *   staleness instead of per-navigation 304 revalidation.
 * - The dev profile keeps "no-store" everywhere so the save-and-see loop
 *   can never serve a stale copy (Last-Modified is one-second granular).
 */
public class ApplicationConfigCacheControl_UnitTest {

	private final AntPathMatcher pathMatcher = new AntPathMatcher();

	private boolean hasSwrPattern(String path) {
		return Arrays.stream(ApplicationConfig.SWR_ASSET_PATTERNS)
			.anyMatch(pattern -> pathMatcher.match(pattern, path));
	}

	@Test
	public void production_pages_revalidate_but_stay_bfcache_eligible() {
		Assertions.assertEquals("no-cache", ApplicationConfig.pageCacheControl(false).getHeaderValue(),
			"pages must send no-cache: revalidation without blocking the back/forward cache");
	}

	@Test
	public void dev_pages_keep_no_store_for_save_and_see() {
		Assertions.assertEquals("no-store", ApplicationConfig.pageCacheControl(true).getHeaderValue());
	}

	@Test
	public void production_assets_get_bounded_staleness() {
		Assertions.assertEquals("max-age=300, stale-while-revalidate=86400",
			ApplicationConfig.assetCacheControl(false).getHeaderValue());
	}

	@Test
	public void dev_assets_keep_no_store_for_save_and_see() {
		Assertions.assertEquals("no-store", ApplicationConfig.assetCacheControl(true).getHeaderValue());
	}

	@Test
	public void swr_patterns_cover_the_hot_asset_dirs() {
		Assertions.assertTrue(hasSwrPattern("/css/oidf-tokens.css"));
		Assertions.assertTrue(hasSwrPattern("/js/fapi.ui.js"));
		Assertions.assertTrue(hasSwrPattern("/components/cts-navbar.js"));
	}

	@Test
	public void vendor_is_deliberately_not_swr_cached() {
		// Vendored library URLs are not versioned; bumps must take effect on
		// the next navigation, so /vendor/** stays on Last-Modified
		// revalidation. See the SWR_ASSET_PATTERNS comment before widening.
		Assertions.assertFalse(hasSwrPattern("/vendor/lit/lit.js"));
		Assertions.assertFalse(hasSwrPattern("/vendor/monaco-editor/vs/loader.js"));
	}
}
