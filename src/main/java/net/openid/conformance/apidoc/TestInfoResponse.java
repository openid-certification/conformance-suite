package net.openid.conformance.apidoc;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import net.openid.conformance.testmodule.TestModule;

import java.util.Map;

@Schema(description = "Stored information about one test instance. When requested with public=true "
	+ "the same shape is returned minus the 'config' field.")
public record TestInfoResponse(
	@JsonProperty("_id")
	@Schema(description = "Id of the test instance") String id,
	@Schema(description = "Id of the test instance (same value as _id)") String testId,
	@Schema(description = "Test module name", example = "oidcc-server") String testName,
	@Schema(description = "Selected variant values (a plain name-to-value object; a bare string for pre-variant legacy tests)") Map<String, String> variant,
	@Schema(description = "When the test was created, ISO-8601", example = "2026-08-20T13:45:12.345Z") String started,
	@Schema(description = "The test configuration JSON (absent when requested with public=true)") Object config,
	@Schema(description = "Free-text description from the configuration, or null") String description,
	@Schema(description = "Alias used in the test URLs, or null") String alias,
	@Schema(description = "Owner of the test") OwnerId owner,
	@Schema(description = "Id of the plan this test belongs to, or null for a standalone test") String planId,
	@Schema(description = "Current lifecycle status of the test") TestModule.Status status,
	@Schema(description = "Conformance suite version that ran the test") String version,
	@Schema(description = "Summary line of the test module") String summary,
	@Schema(description = "Publication state: null, 'summary' or 'everything'", allowableValues = {"summary", "everything"}) String publish,
	@Schema(description = "Current result of the test, or null if not yet known") TestModule.Result result) {
}
