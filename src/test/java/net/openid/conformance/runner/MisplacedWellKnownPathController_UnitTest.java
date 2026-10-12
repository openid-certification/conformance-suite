package net.openid.conformance.runner;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A client that uses a well-known name as the first path segment instead of placing
 * "/.well-known/<name>" where the metadata's specification puts it (issue #1817) must be told
 * where the metadata really is, rather than redirected to the login page.
 */
public class MisplacedWellKnownPathController_UnitTest {

	private static final String BASE = "https://localhost.emobix.co.uk:8443";

	private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new MisplacedWellKnownPathController(BASE)).build();

	private MockHttpServletResponse get(String path, String accept) throws Exception {
		return mvc.perform(MockMvcRequestBuilders.get(path).header("Accept", accept)).andReturn().getResponse();
	}

	private MockHttpServletResponse get(String path) throws Exception {
		return get(path, "application/json");
	}

	private void assertHint(String path, String expectedUrl, String rule) throws Exception {
		MockHttpServletResponse response = get(path);

		assertThat(response.getStatus()).as(path).isEqualTo(HttpStatus.NOT_FOUND.value());
		assertThat(response.getContentAsString()).as(path)
			.contains("\"expected_url\":\"" + expectedUrl + "\"")
			.contains(rule);
	}

	@Test
	public void inserts_the_name_after_the_host_for_rfc8414_metadata() throws Exception {
		assertHint("/oauth-authorization-server/test/a/my-wallet/",
			BASE + "/.well-known/oauth-authorization-server/test/a/my-wallet", "RFC 8414 section 3.1");
		assertHint("/openid-credential-issuer/test/a/my-wallet",
			BASE + "/.well-known/openid-credential-issuer/test/a/my-wallet", "RFC 8414 section 3.1");
		assertHint("/ssf-configuration/test/a/my-transmitter",
			BASE + "/.well-known/ssf-configuration/test/a/my-transmitter", "RFC 8414 section 3.1");
	}

	@Test
	public void appends_the_name_to_the_issuer_path_for_openid_provider_configuration() throws Exception {
		// the suite serves /test/a/<alias>/.well-known/openid-configuration, not the RFC 8414 layout
		assertHint("/openid-configuration/test/a/demo/",
			BASE + "/test/a/demo/.well-known/openid-configuration", "OpenID Connect Discovery 1.0 section 4.1");
	}

	@Test
	public void appends_the_name_to_the_entity_identifier_for_federation_entity_configuration() throws Exception {
		assertHint("/openid-federation/test/a/demo",
			BASE + "/test/a/demo/.well-known/openid-federation", "OpenID Federation 1.0 section 9");
	}

	@Test
	public void handles_the_bare_well_known_name() throws Exception {
		// no issuer path: both layouts collapse to the same URL
		assertHint("/openid-configuration", BASE + "/.well-known/openid-configuration", "appending");
		assertHint("/oauth-authorization-server/", BASE + "/.well-known/oauth-authorization-server", "inserting");
	}

	@Test
	public void answers_json_whatever_the_client_accepts() throws Exception {
		for (String accept : new String[] {"text/html", "application/jwt", "application/json", "*/*"}) {
			MockHttpServletResponse response = get("/oauth-authorization-server/test/a/my-wallet/", accept);

			assertThat(response.getStatus()).as("Accept: " + accept).isEqualTo(HttpStatus.NOT_FOUND.value());
			assertThat(response.getContentType()).as("Accept: " + accept).startsWith(MediaType.APPLICATION_JSON_VALUE);
			assertThat(response.getContentAsString()).as("Accept: " + accept)
				.contains("/.well-known/oauth-authorization-server/test/a/my-wallet");
		}
	}

	@Test
	public void ignores_names_the_suite_does_not_serve() throws Exception {
		// the suite is the client of a PDP, never its server, so there is no URL to point at
		MockHttpServletResponse response = get("/authzen-configuration/test/a/demo");

		assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
		assertThat(response.getContentAsString()).isEmpty();
	}
}
