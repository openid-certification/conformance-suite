package net.openid.conformance.apidoc;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Result of a publish request")
public record PublishResponse(
	@Schema(description = "Id of the published test or plan") String id,
	@Schema(description = "The publication state that was set", allowableValues = {"summary", "everything"}) String publish) {
}
