package net.openid.conformance.apidoc;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Error body")
public record ErrorResponse(
	@Schema(description = "Human-readable description of what went wrong") String error) {
}
