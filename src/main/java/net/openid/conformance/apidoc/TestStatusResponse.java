package net.openid.conformance.apidoc;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

@Schema(description = "Live state of a running test instance. Note this does not include the "
	+ "test's status or result — poll GET /api/info/{id} (or /api/runner/{id}/wait-state) for those.")
public record TestStatusResponse(
	@Schema(description = "Test module name") String name,
	@Schema(description = "Id of the test instance") String id,
	@Schema(description = "Name-to-value strings the test exposes to the user (e.g. client_id)") Map<String, String> exposed,
	@Schema(description = "Owner of the test") OwnerId owner,
	@Schema(description = "When the test was created, ISO-8601") String created,
	@Schema(description = "When the test's status last changed, ISO-8601") String updated,
	@Schema(description = "Details of the test's final error, or null when there is none") Object error,
	@Schema(description = "Front-channel interaction state; absent for tests without a browser component") BrowserDetail browser) {

	@Schema(description = "Front-channel interaction state of a running test")
	public record BrowserDetail(
		@JsonProperty("show_qr_code")
		@Schema(description = "Whether the UI should render the URLs as QR codes (wallet tests)") boolean showQrCode,
		@Schema(description = "URLs the tester (or an automated browser) still needs to visit") List<String> urls,
		@Schema(description = "As 'urls', each with the HTTP method to use") List<Object> urlsWithMethod,
		@Schema(description = "Pending browser-API interactions, each with the request object and its submission URL") List<Object> browserApiRequests,
		@Schema(description = "Pending requests for the tester to paste a URI, each with a submission URL and description") List<Object> uriInputRequests,
		@Schema(description = "URLs already visited") List<String> visited,
		@Schema(description = "As 'visited', each with the HTTP method used") List<Object> visitedUrlsWithMethod,
		@Schema(description = "State of the automated browser runners processing the URLs") List<Object> runners) {
	}
}
