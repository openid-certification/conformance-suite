package net.openid.conformance.apidoc;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "Certification package could not be prepared because tests failed or are "
	+ "incomplete (or the plan id is unknown). Note: a 422 can also be returned with an empty "
	+ "body when the plan could not be marked immutable.")
public record CertificationPackageErrorResponse(
	@Schema(description = "Error code", example = "failed_or_incomplete_tests") String error,
	@JsonProperty("error_description")
	@Schema(description = "Human-readable description") String errorDescription,
	@JsonProperty("plan_name")
	@Schema(description = "Name of the plan") String planName,
	@JsonProperty("test_plan_id")
	@Schema(description = "Id of the plan instance") String testPlanId,
	@Schema(description = "The plan's variant selection, as a string") String variant,
	@JsonProperty("failed_tests")
	@Schema(description = "Per-module details of the failing or incomplete tests, keyed by module name") Map<String, Object> failedTests) {
}
