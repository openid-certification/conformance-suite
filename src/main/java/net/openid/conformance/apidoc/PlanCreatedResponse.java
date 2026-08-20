package net.openid.conformance.apidoc;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

@Schema(description = "Result of creating a test plan instance")
public record PlanCreatedResponse(
	@Schema(description = "The plan name that was requested", example = "oidcc-basic-certification-test-plan") String name,
	@Schema(description = "Id of the new test plan instance", example = "Wt3aZAz6PYLOr") String id,
	@Schema(description = "The test modules the plan will run, in order") List<ModuleEntry> modules) {

	@Schema(description = "One test module within a plan")
	public record ModuleEntry(
		@Schema(description = "Test module name", example = "oidcc-server") String testModule,
		@Schema(description = "Module-specific variant values, if any (null otherwise)") Map<String, String> variant,
		@Schema(description = "Ids of test instances already run for this module (empty at creation)") List<String> instances) {
	}
}
