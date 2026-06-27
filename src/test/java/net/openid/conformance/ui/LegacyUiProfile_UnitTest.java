package net.openid.conformance.ui;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;

import java.io.InputStream;
import java.util.Properties;

/**
 * Guards the {@code legacy-ui} escape-hatch wiring (the frozen pre-redesign UI). See
 * {@link LegacyUiConfig}.
 *
 * <p>Plain unit tests by repo convention (cf. {@code StaticAssetExemption_UnitTest},
 * {@code HomeController_UnitTest}). The actual HTTP serving precedence / welcome-page behaviour
 * is validated on the running server, not here: a full {@code @SpringBootTest} is not
 * Mongo-independent ({@code Application.doPostConstruct()} creates DB indexes and a ready
 * listener queries Mongo), and a MockMvc request would trip {@code RejectPlainHttpTrafficFilter}
 * unless marked secure. These tests therefore lock the two invariants that ARE unit-testable:
 * the profile's static-location override, and that {@code HomeController} is gated off so the
 * welcome page can take over.</p>
 */
public class LegacyUiProfile_UnitTest {

	@Test
	public void legacyProfile_repoints_static_locations_at_the_snapshot_only() throws Exception {
		Properties props = new Properties();
		try (InputStream in = getClass().getResourceAsStream("/application-legacy-ui.properties")) {
			Assertions.assertNotNull(in, "application-legacy-ui.properties must be on the classpath");
			props.load(in);
		}
		String locations = props.getProperty("spring.web.resources.static-locations");
		Assertions.assertNotNull(locations, "legacy profile must set spring.web.resources.static-locations");
		Assertions.assertTrue(locations.startsWith("classpath:/static-legacy/"),
			"static-legacy/ must be first so the frozen snapshot wins: " + locations);
		// The new-UI tree must be absent so legacy mode is a clean, self-contained snapshot:
		// a path missing from static-legacy/ 404s rather than silently borrowing a current-UI asset.
		Assertions.assertFalse(locations.contains("classpath:/static/"),
			"classpath:/static/ (new UI) must be omitted from the legacy chain: " + locations);
	}

	@Test
	public void homeController_is_disabled_under_legacy_ui() {
		Profile profile = HomeController.class.getAnnotation(Profile.class);
		Assertions.assertNotNull(profile,
			"HomeController must be @Profile-gated so `/` falls back to the welcome page under legacy-ui");
		Assertions.assertArrayEquals(new String[]{"!legacy-ui"}, profile.value(),
			"HomeController must be excluded under the legacy-ui profile");
	}

	@Test
	public void legacyUiConfig_is_scoped_to_the_legacy_ui_profile() {
		Profile profile = LegacyUiConfig.class.getAnnotation(Profile.class);
		Assertions.assertNotNull(profile, "LegacyUiConfig must be @Profile(\"legacy-ui\")");
		Assertions.assertArrayEquals(new String[]{"legacy-ui"}, profile.value());
	}
}
