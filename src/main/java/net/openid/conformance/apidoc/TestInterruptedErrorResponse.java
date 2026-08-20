package net.openid.conformance.apidoc;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Error body produced when a request causes a running test to fail or be "
	+ "skipped (500 for a general failure, 400 for a test failure carrying an OAuth-style error, "
	+ "200 when the test was skipped)")
public record TestInterruptedErrorResponse(
	@Schema(description = "Error message, or an OAuth-style error code for a test failure") String error,
	@JsonProperty("error_description")
	@Schema(description = "Description accompanying an OAuth-style error; absent otherwise") String errorDescription,
	@Schema(description = "Message of the underlying cause, or null") String cause,
	@Schema(description = "Id of the affected test instance") String testId) {
}
