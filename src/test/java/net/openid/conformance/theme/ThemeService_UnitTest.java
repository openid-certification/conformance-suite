package net.openid.conformance.theme;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spike-level coverage of the pure parts of {@link ThemeService}: CSS generation
 * and theme validation. The Mongo/file providers are exercised manually via the
 * journeys in docs/theming-spike/.
 */
public class ThemeService_UnitTest {

	private ThemeService service;

	@BeforeEach
	public void setUp() {
		service = new ThemeService();
	}

	private JsonObject theme(String json) {
		return JsonParser.parseString(json).getAsJsonObject();
	}

	@Test
	public void generateCss_derivesRampAndRepinsWarning() {
		String css = service.generateCss(theme("""
			{"partner": {"name": "HelseID"}, "brand": {"accent": "#0067C5"}}
			"""));

		assertThat(css).contains("--theme-accent: #0067C5;");
		assertThat(css).contains("--orange-400: var(--theme-accent);");
		assertThat(css).contains("--orange-500: color-mix(in oklab, var(--theme-accent), black 14%);");
		assertThat(css).contains("--focus-ring: 0 0 0 3px color-mix(in srgb, var(--theme-accent) 35%, transparent);");
		// status colors are semantic, not brand — the accent must not leak into them
		assertThat(css).contains("--status-warning: #D27420;");
	}

	@Test
	public void generateCss_explicitRampStepWinsOverDerived() {
		String css = service.generateCss(theme("""
			{"brand": {"accent": "#0067C5", "ramp": {"500": "#004A8F"}}}
			"""));

		assertThat(css).contains("--orange-500: #004A8F;");
		assertThat(css).contains("--orange-600: color-mix(in oklab, var(--theme-accent), black 30%);");
	}

	@Test
	public void generateCss_noThemeYieldsCommentOnly() {
		assertThat(service.generateCss(null)).doesNotContain(":root");
	}

	@Test
	public void validate_acceptsMinimalTheme() {
		List<String> errors = service.validate(theme("""
			{"partner": {"name": "HelseID"}, "brand": {"accent": "#0067C5"}}
			"""));

		assertThat(errors).isEmpty();
	}

	@Test
	public void validate_rejectsBadAccentAndMissingName() {
		List<String> errors = service.validate(theme("""
			{"partner": {"name": ""}, "brand": {"accent": "blue"}}
			"""));

		assertThat(errors).anyMatch(e -> e.contains("partner.name"));
		assertThat(errors).anyMatch(e -> e.contains("brand.accent"));
	}

	@Test
	public void validate_rejectsScriptInSvgLogo() {
		String svg = Base64.getEncoder().encodeToString(
			"<svg xmlns='http://www.w3.org/2000/svg'><script>alert(1)</script></svg>".getBytes(StandardCharsets.UTF_8));
		List<String> errors = service.validate(theme("""
			{"partner": {"name": "P"}, "brand": {"accent": "#0067C5",
			 "logo": {"data": "data:image/svg+xml;base64,%s", "alt": "P"}}}
			""".formatted(svg)));

		assertThat(errors).anyMatch(e -> e.contains("scripts"));
	}

	@Test
	public void validate_rejectsDuplicateAndMalformedPresetIds() {
		List<String> errors = service.validate(theme("""
			{"partner": {"name": "P"}, "brand": {"accent": "#0067C5"},
			 "presets": [
			   {"id": "rp-test", "label": "RP", "planName": "x"},
			   {"id": "rp-test", "label": "RP again", "planName": "x"},
			   {"id": "Bad Id!", "label": "Nope", "planName": "x"}
			 ]}
			"""));

		assertThat(errors).anyMatch(e -> e.contains("duplicate preset id 'rp-test'"));
		assertThat(errors).anyMatch(e -> e.contains("'Bad Id!'"));
	}
}
