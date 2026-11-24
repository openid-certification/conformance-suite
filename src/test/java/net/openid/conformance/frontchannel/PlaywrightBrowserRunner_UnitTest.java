package net.openid.conformance.frontchannel;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import net.openid.conformance.frontchannel.PlaywrightBrowserRunner.TraceMode;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PlaywrightBrowserRunner_UnitTest {

	@Test
	public void autoSubmittingFormDecodesParametersAndEscapesHtml() {
		String html = PlaywrightBrowserRunner.buildAutoSubmittingForm(
			"https://as.example.com/authorize?x=1&y=\"2\"",
			"response_type=code&scope=openid%20profile&state=a%26b&redirect_uri=https%3A%2F%2Frp.example.com%2Fcb&empty=&flag");

		assertThat(html).startsWith("<!DOCTYPE html>");
		assertThat(html).contains("onload=\"setTimeout(function(){HTMLFormElement.prototype.submit.call(document.forms[0]);},0)\"");
		assertThat(html).contains("<form method=\"POST\" action=\"https://as.example.com/authorize?x=1&amp;y=&quot;2&quot;\">");
		assertThat(html).contains("<input type=\"hidden\" name=\"response_type\" value=\"code\">");
		assertThat(html).contains("<input type=\"hidden\" name=\"scope\" value=\"openid profile\">");
		assertThat(html).contains("<input type=\"hidden\" name=\"state\" value=\"a&amp;b\">");
		assertThat(html).contains("<input type=\"hidden\" name=\"redirect_uri\" value=\"https://rp.example.com/cb\">");
		assertThat(html).contains("<input type=\"hidden\" name=\"empty\" value=\"\">");
		assertThat(html).contains("<input type=\"hidden\" name=\"flag\" value=\"\">");
	}

	@Test
	public void autoSubmittingFormWithoutParametersHasNoInputs() {
		String html = PlaywrightBrowserRunner.buildAutoSubmittingForm("https://as.example.com/authorize", null);

		assertThat(html).contains("<form method=\"POST\" action=\"https://as.example.com/authorize\"></form>");
		assertThat(html).doesNotContain("<input");
	}

	@Test
	public void thePinnedPlaywrightVersionStillHasTheDriverProcessFieldTheShutdownWatchdogReads() throws Exception {
		// fails on a Playwright update that renames or retypes the field: without it a hung
		// browser shutdown can no longer be terminated, see PlaywrightBrowserRunner.closeBrowser()
		assertThat(PlaywrightBrowserRunner.driverProcessField().getType()).isEqualTo(Process.class);
	}

	@Test
	public void theSharedBrowsersEndpointIsLoggedWithoutItsSecretPath() {
		assertThat(PlaywrightBrowserServer.withoutSecret("ws://[::1]:46837/23b8f6fcbdcad9085303485923991931"))
			.isEqualTo("ws://[::1]:46837/...");
		assertThat(PlaywrightBrowserServer.withoutSecret("ws://127.0.0.1:1234/abc")).isEqualTo("ws://127.0.0.1:1234/...");
	}

	@Test
	public void anOptionalTaskIsOnlySkippedAtOnceWhenTheAddressIsALaterTasks() {
		List<String> later = List.of("*/consent*", "*/test/*/callback*");

		assertThat(PlaywrightBrowserRunner.isOnALaterTasksPage(later, "https://suite.example.com/test/a/alias/callback?code=1")).isTrue();
		assertThat(PlaywrightBrowserRunner.isOnALaterTasksPage(later, "https://suite.example.com/test/a/alias/callback#code=1")).isTrue();
		assertThat(PlaywrightBrowserRunner.isOnALaterTasksPage(later, "https://as.example.com/consent")).isTrue();
		assertThat(PlaywrightBrowserRunner.isOnALaterTasksPage(later, "https://as.example.com/login")).isFalse();

		// where the browser is headed is not where it is: the authorization request and the login
		// page carry the callback's url as a parameter
		assertThat(PlaywrightBrowserRunner.isOnALaterTasksPage(later,
			"https://as.example.com/auth?client_id=x&redirect_uri=https://suite.example.com/test/a/alias/callback")).isFalse();
		assertThat(PlaywrightBrowserRunner.isOnALaterTasksPage(later,
			"https://as.example.com/login#return=https://suite.example.com/test/a/alias/callback")).isFalse();
		assertThat(PlaywrightBrowserRunner.isOnALaterTasksPage(List.of("*callback*"),
			"https://as.example.com/auth?redirect_uri=https%3A%2F%2Fsuite.example.com%2Fcallback")).isFalse();

		assertThat(PlaywrightBrowserRunner.isOnALaterTasksPage(List.of(), "https://as.example.com/login")).isFalse();
	}

	@Test
	public void waitContainsAcceptsEveryUrlHtmlUnitsLiteralTestAcceptsAndWildcardsBesides() {
		String url = "https://suite.example.com/test/a/alias/post?state=a*b&x=1";

		// literal substrings, the HtmlUnit runner's semantics (Selenium's urlContains)
		for (String literal : new String[] {"/test/a/alias/post", "suite.example.com", "state=a*b", "?state", "x=1", ""}) {
			assertThat(url.contains(literal)).isTrue();
			assertThat(PlaywrightBrowserRunner.urlContains(literal, url)).as("literal %s", literal).isTrue();
		}
		// '*' matches any run of characters, as the CI configuration's "/test/*/post" needs
		assertThat(PlaywrightBrowserRunner.urlContains("/test/*/post", url)).isTrue();
		assertThat(PlaywrightBrowserRunner.urlContains("/test/*/post", "https://suite.example.com/test/a/alias/callback")).isFalse();
		// nothing else is special: no regular expressions, no '?' wildcard
		assertThat(PlaywrightBrowserRunner.urlContains("/test/.*/post", url)).isFalse();
		assertThat(PlaywrightBrowserRunner.urlContains("alias/pos?", url)).isFalse();
		assertThat(PlaywrightBrowserRunner.urlContains("/callback", url)).isFalse();
	}

	@Test
	public void laterTaskUrlMatchersLeaveOutThoseMatchingAnything() {
		JsonArray tasks = JsonParser.parseString("""
			[
				{"task": "Login", "match": "*/login*"},
				{"task": "Consent", "match": "*/consent*"},
				{"task": "Anything"},
				{"task": "Star", "match": "*"},
				{"task": "Stars", "match": "**"},
				{"task": "Empty", "match": ""},
				{"task": "Done", "match": "*/callback*"}
			]""").getAsJsonArray();

		assertThat(PlaywrightBrowserRunner.laterTaskUrlMatchers(tasks, 0)).containsExactly("*/consent*", "*/callback*");
		assertThat(PlaywrightBrowserRunner.laterTaskUrlMatchers(tasks, 1)).containsExactly("*/callback*");
		assertThat(PlaywrightBrowserRunner.laterTaskUrlMatchers(tasks, 6)).isEmpty();
	}

	@Test
	public void traceModeParsesConfiguredValues() {
		assertThat(TraceMode.parse("false")).isEqualTo(TraceMode.OFF);
		assertThat(TraceMode.parse("")).isEqualTo(TraceMode.OFF);
		assertThat(TraceMode.parse("nonsense")).isEqualTo(TraceMode.OFF);
		assertThat(TraceMode.parse("true")).isEqualTo(TraceMode.ALWAYS);
		assertThat(TraceMode.parse("always")).isEqualTo(TraceMode.ALWAYS);
		assertThat(TraceMode.parse("on-failure")).isEqualTo(TraceMode.ON_FAILURE);
		assertThat(TraceMode.parse("On-Failure")).isEqualTo(TraceMode.ON_FAILURE);
	}

	@Test
	public void settingsDefaultWhenNothingIsConfigured() {
		PlaywrightBrowserRunner.Settings defaults = PlaywrightBrowserRunner.Settings.from(name -> null);

		assertThat(defaults.browserType()).isEqualTo("chromium");
		assertThat(defaults.headless()).isTrue();
		assertThat(defaults.slowMo()).isZero();
		assertThat(defaults.sharedBrowser()).isTrue();
		assertThat(defaults.extraHttpHeaders()).isEmpty();
		assertThat(defaults.traceMode()).isEqualTo(TraceMode.OFF);
		assertThat(defaults.tracesDir()).isEmpty();
	}

	@Test
	public void settingsAreReadByTheirFullNames() {
		Map<String, String> configured = new HashMap<>(Map.of(
			"browser.playwright.type", "Firefox",
			"browser.playwright.headless", "false",
			"browser.playwright.slowMo", "250",
			"browser.playwright.sharedBrowser", "false",
			"browser.playwright.extraHttpHeaders", "{\"X-Test\": \"1\", \"ngrok-skip-browser-warning\": \"yes\"}",
			"browser.playwright.traceEnabled", "on-failure",
			"browser.playwright.tracesDir", "/tmp/traces"));

		PlaywrightBrowserRunner.Settings settings = PlaywrightBrowserRunner.Settings.from(configured::get);

		assertThat(settings.browserType()).isEqualTo("firefox");
		assertThat(settings.headless()).isFalse();
		assertThat(settings.slowMo()).isEqualTo(250);
		assertThat(settings.sharedBrowser()).isFalse();
		assertThat(settings.extraHttpHeaders()).containsExactlyInAnyOrderEntriesOf(
			Map.of("X-Test", "1", "ngrok-skip-browser-warning", "yes"));
		assertThat(settings.traceMode()).isEqualTo(TraceMode.ON_FAILURE);
		assertThat(settings.tracesDir()).isEqualTo("/tmp/traces");

		configured.put("browser.playwright.extraHttpHeaders", "not json");
		assertThat(PlaywrightBrowserRunner.Settings.from(configured::get).extraHttpHeaders()).isEmpty();
	}

	@Test
	public void browserSettingsDefaultToSeleniumAndRejectAnUnknownEngine() {
		assertThat(new BrowserSettings(name -> null).engine()).isEqualTo(BrowserSettings.ENGINE_SELENIUM);
		assertThat(new BrowserSettings(name -> null).isPlaywright()).isFalse();

		BrowserSettings playwright = new BrowserSettings(Map.of(
			"browser.engine", "Playwright",
			"browser.playwright.tracesDir", "/tmp/traces")::get);
		assertThat(playwright.isPlaywright()).isTrue();
		assertThat(playwright.tracesDir()).isEqualTo("/tmp/traces");

		assertThatThrownBy(() -> new BrowserSettings(Map.of("browser.engine", "netscape")::get))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("netscape");
	}
}
