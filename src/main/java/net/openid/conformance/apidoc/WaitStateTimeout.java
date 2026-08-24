package net.openid.conformance.apidoc;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The wait timed out before the test reached any of the requested states")
public record WaitStateTimeout(
	@Schema(description = "Always true") boolean timeout) {
}
