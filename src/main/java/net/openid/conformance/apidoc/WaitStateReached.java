package net.openid.conformance.apidoc;

import io.swagger.v3.oas.annotations.media.Schema;
import net.openid.conformance.testmodule.TestModule;

@Schema(description = "The test reached one of the requested states")
public record WaitStateReached(
	@Schema(description = "The status the test is now in") TestModule.Status state) {
}
