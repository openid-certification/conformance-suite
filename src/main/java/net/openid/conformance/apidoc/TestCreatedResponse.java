package net.openid.conformance.apidoc;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Result of creating a test module instance")
public record TestCreatedResponse(
	@Schema(description = "Test module name", example = "oidcc-server") String name,
	@Schema(description = "Id of the new test instance; used with /api/info/{id}, /api/runner/{id} and /api/log/{id}", example = "qX3wbqjcv0e6qFz") String id,
	@Schema(description = "Base URL of this test instance's endpoints") String url) {
}
