package net.openid.conformance.apidoc;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Front-channel URL state for a running test (a subset of the 'browser' "
	+ "object returned by GET /api/runner/{id})")
public record BrowserStatusResponse(
	@Schema(description = "Id of the test instance") String id,
	@JsonProperty("show_qr_code")
	@Schema(description = "Whether the UI should render the URLs as QR codes (wallet tests)") boolean showQrCode,
	@Schema(description = "URLs the tester (or an automated browser) still needs to visit") List<String> urls,
	@Schema(description = "URLs already visited") List<String> visited,
	@Schema(description = "State of the automated browser runners processing the URLs") List<Object> runners) {
}
