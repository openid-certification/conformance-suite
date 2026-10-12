package net.openid.conformance.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The pages the suite serves to a browser are the only paths worth a login redirect; every
 * other anonymous request is a 404, so a probe or a malformed discovery URL is never mistaken
 * for a page that needs a login.
 */
public class ServedPageRequestMatcher_UnitTest {

	private final ServedPageRequestMatcher matcher = new ServedPageRequestMatcher("static");

	private boolean matches(String method, String path) {
		return matcher.matches(new MockHttpServletRequest(method, path));
	}

	@Test
	public void matches_the_home_page_and_html_pages_that_exist() {
		assertThat(matches("GET", "/")).isTrue();
		assertThat(matches("GET", "/index.html")).isTrue();
		assertThat(matches("GET", "/schedule-test.html")).isTrue();
		assertThat(matches("GET", "/statistics.html")).isTrue();
	}

	@Test
	public void does_not_match_paths_with_no_page_behind_them() {
		assertThat(matches("GET", "/nonexistent.html")).isFalse();
		assertThat(matches("GET", "/some/random/path")).isFalse();
		assertThat(matches("GET", "/oauth-authorization-server/test/a/alias/")).isFalse();
		assertThat(matches("GET", "/css/main.css")).isFalse();
		assertThat(matches("GET", "/components/cts-time.js")).isFalse();
	}

	@Test
	public void does_not_match_lookups_outside_the_page_directory() {
		assertThat(matches("GET", "/../application.properties")).isFalse();
		assertThat(matches("GET", "/schedule-test.html/")).isFalse();
	}

	@Test
	public void only_matches_get_and_head() {
		assertThat(matches("HEAD", "/schedule-test.html")).isTrue();
		assertThat(matches("POST", "/schedule-test.html")).isFalse();
	}

	@Test
	public void follows_the_configured_page_directory() {
		ServedPageRequestMatcher legacy = new ServedPageRequestMatcher("static-legacy");
		assertThat(legacy.matches(new MockHttpServletRequest("GET", "/plans.html"))).isTrue();
		// settings.html only exists in the redesigned UI
		assertThat(legacy.matches(new MockHttpServletRequest("GET", "/settings.html"))).isFalse();
		assertThat(matches("GET", "/settings.html")).isTrue();
	}
}
