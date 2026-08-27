package net.openid.conformance.frontchannel;

import net.openid.conformance.frontchannel.PlaywrightBrowserRunner.TraceMode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PlaywrightBrowserRunner_UnitTest {

	@Test
	public void autoSubmittingFormDecodesParametersAndEscapesHtml() {
		String html = PlaywrightBrowserRunner.buildAutoSubmittingForm(
			"https://as.example.com/authorize?x=1&y=\"2\"",
			"response_type=code&scope=openid%20profile&state=a%26b&redirect_uri=https%3A%2F%2Frp.example.com%2Fcb&empty=&flag");

		assertThat(html).startsWith("<!DOCTYPE html>");
		assertThat(html).contains("onload=\"setTimeout(function(){document.forms[0].submit();},0)\"");
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
	public void settingsAreReadFromSystemProperties() {
		String[] names = {"browser.playwright.type", "browser.playwright.headless", "browser.playwright.slowMo",
			"browser.playwright.sharedBrowser", "browser.playwright.extraHttpHeaders", "browser.playwright.traceEnabled",
			"browser.playwright.tracesDir"};
		try {
			PlaywrightBrowserRunner.Settings defaults = PlaywrightBrowserRunner.Settings.fromSystemProperties();
			assertThat(defaults.browserType()).isEqualTo("chromium");
			assertThat(defaults.headless()).isTrue();
			assertThat(defaults.slowMo()).isZero();
			assertThat(defaults.sharedBrowser()).isTrue();
			assertThat(defaults.extraHttpHeaders()).isEmpty();
			assertThat(defaults.traceMode()).isEqualTo(TraceMode.OFF);
			assertThat(defaults.tracesDir()).isEmpty();

			System.setProperty("browser.playwright.type", "Firefox");
			System.setProperty("browser.playwright.headless", "false");
			System.setProperty("browser.playwright.slowMo", "250");
			System.setProperty("browser.playwright.sharedBrowser", "false");
			System.setProperty("browser.playwright.extraHttpHeaders", "{\"X-Test\": \"1\", \"ngrok-skip-browser-warning\": \"yes\"}");
			System.setProperty("browser.playwright.traceEnabled", "on-failure");
			System.setProperty("browser.playwright.tracesDir", "/tmp/traces");

			PlaywrightBrowserRunner.Settings configured = PlaywrightBrowserRunner.Settings.fromSystemProperties();
			assertThat(configured.browserType()).isEqualTo("firefox");
			assertThat(configured.headless()).isFalse();
			assertThat(configured.slowMo()).isEqualTo(250);
			assertThat(configured.sharedBrowser()).isFalse();
			assertThat(configured.extraHttpHeaders()).containsExactlyInAnyOrderEntriesOf(
				java.util.Map.of("X-Test", "1", "ngrok-skip-browser-warning", "yes"));
			assertThat(configured.traceMode()).isEqualTo(TraceMode.ON_FAILURE);
			assertThat(configured.tracesDir()).isEqualTo("/tmp/traces");

			System.setProperty("browser.playwright.extraHttpHeaders", "not json");
			assertThat(PlaywrightBrowserRunner.Settings.fromSystemProperties().extraHttpHeaders()).isEmpty();
		} finally {
			for (String name : names) {
				System.clearProperty(name);
			}
		}
	}
}
