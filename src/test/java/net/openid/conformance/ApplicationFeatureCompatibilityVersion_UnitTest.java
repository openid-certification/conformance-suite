package net.openid.conformance;

import org.bson.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ApplicationFeatureCompatibilityVersion_UnitTest {

	@Test
	public void versions_compare_numerically_not_as_strings() {
		Assertions.assertTrue(Application.compareFeatureCompatibilityVersions("6.0", "7.0") < 0);
		Assertions.assertTrue(Application.compareFeatureCompatibilityVersions("7.0", "6.0") > 0);
		Assertions.assertEquals(0, Application.compareFeatureCompatibilityVersions("7.0", "7.0"));
		// "10.0" sorts before "9.0" as a string
		Assertions.assertTrue(Application.compareFeatureCompatibilityVersions("9.0", "10.0") < 0);
		Assertions.assertTrue(Application.compareFeatureCompatibilityVersions("8.0", "8.3") < 0);
	}

	@Test
	public void a_malformed_version_is_rejected() {
		Assertions.assertThrows(IllegalArgumentException.class,
			() -> Application.compareFeatureCompatibilityVersions("7", "7.0"));
	}

	@Test
	public void the_command_for_7_0_or_later_is_confirmed() {
		Assertions.assertEquals(
			new Document("setFeatureCompatibilityVersion", "7.0").append("confirm", true),
			Application.setFeatureCompatibilityVersionCommand("7.0"));
		Assertions.assertEquals(
			new Document("setFeatureCompatibilityVersion", "8.0").append("confirm", true),
			Application.setFeatureCompatibilityVersionCommand("8.0"));
	}

	@Test
	public void the_command_before_7_0_has_no_confirm_field() {
		Assertions.assertEquals(
			new Document("setFeatureCompatibilityVersion", "6.0"),
			Application.setFeatureCompatibilityVersionCommand("6.0"));
	}
}
