package net.openid.conformance.runner;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A wallet that builds the RFC 8414 well-known URL by appending the well-known name to the
 * host instead of inserting "/.well-known/" between host and path (issue #1817) must be told
 * where the metadata really is, rather than redirected to the login page.
 */
public class MisplacedWellKnownPathController_UnitTest {

	private final MisplacedWellKnownPathController controller =
		new MisplacedWellKnownPathController("https://localhost.emobix.co.uk:8443");

	private ResponseEntity<Map<String, String>> get(String path) {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
		return controller.handle(request);
	}

	@Test
	public void points_at_the_rfc8414_url_for_an_oauth_as_metadata_request() {
		ResponseEntity<Map<String, String>> response = get("/oauth-authorization-server/test/a/my-wallet/");

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody()).containsEntry("expected_url",
			"https://localhost.emobix.co.uk:8443/.well-known/oauth-authorization-server/test/a/my-wallet");
		assertThat(response.getBody().get("error"))
			.contains("/.well-known/oauth-authorization-server")
			.contains("RFC 8414");
	}

	@Test
	public void points_at_the_rfc8414_url_for_a_credential_issuer_metadata_request() {
		ResponseEntity<Map<String, String>> response = get("/openid-credential-issuer/test/a/my-wallet");

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody()).containsEntry("expected_url",
			"https://localhost.emobix.co.uk:8443/.well-known/openid-credential-issuer/test/a/my-wallet");
	}

	@Test
	public void handles_the_bare_well_known_name() {
		ResponseEntity<Map<String, String>> response = get("/openid-configuration");

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody()).containsEntry("expected_url",
			"https://localhost.emobix.co.uk:8443/.well-known/openid-configuration");
	}
}
