package net.openid.conformance;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.util.AntPathMatcher;

import java.util.Arrays;

/**
 * Guards the cache policy split in {@link ApplicationConfig}:
 *
 * - Pages send "no-store" in every profile: they never enter the back/forward
 *   cache, so logout needs no Clear-Site-Data purge, which stalled the logout
 *   redirect for as long as the browser took to clear the origin's cache.
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
	public void production_pages_are_never_stored() {
		Assertions.assertEquals("no-store", ApplicationConfig.pageCacheControl(false).getHeaderValue(),
			"pages must send no-store so Back cannot restore an authenticated shell after logout");
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
