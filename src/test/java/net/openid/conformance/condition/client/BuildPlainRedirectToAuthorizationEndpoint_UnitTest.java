package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class BuildPlainRedirectToAuthorizationEndpoint_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private JsonObject server;

	private JsonObject authorizationEndpointRequest;

	private BuildPlainRedirectToAuthorizationEndpoint cond;

	private String clientId = "s6BhdRkqt3";

	private String state = "xyz123";

	private String scope = "address phone openid email profile";

	private String redirectUri = "https://client.example.com/cb";

	private String responseType = "code";

	private String authorizationEndpoint = "https://example.com/oauth/authorize";

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	public void setUp() throws Exception {

		cond = new BuildPlainRedirectToAuthorizationEndpoint();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		server = new JsonObject();
		server.addProperty("authorization_endpoint", authorizationEndpoint);
		env.putObject("server", server);
	}

	/**
	 * Test method for {@link BuildPlainRedirectToAuthorizationEndpoint#evaluate(Environment)}.
	 *
	 * @throws UnsupportedEncodingException
	 */
	@Test
	public void testEvaluate() throws UnsupportedEncodingException {
		authorizationEndpointRequest = new JsonObject();
		authorizationEndpointRequest.addProperty("client_id", clientId);
		authorizationEndpointRequest.addProperty("redirect_uri", redirectUri);
		authorizationEndpointRequest.addProperty("scope", scope);
		authorizationEndpointRequest.addProperty("state", state);
		authorizationEndpointRequest.addProperty("response_type", responseType);

		env.putObject("authorization_endpoint_request", authorizationEndpointRequest);

		cond.execute(env);

		verify(env, atLeastOnce()).getString("server", "authorization_endpoint");
		verify(env, atLeastOnce()).getObject("authorization_endpoint_request");

		assertThat(env.getString("redirect_to_authorization_endpoint")).startsWith(authorizationEndpoint);

		UriComponents redirectUriComponents = UriComponentsBuilder.fromUriString(env.getString("redirect_to_authorization_endpoint")).build();
		Map<String, List<String>> redirectUriParams = redirectUriComponents.getQueryParams();

		assertThat(redirectUriParams.get("response_type")).containsExactly(UriUtils.encode(responseType, StandardCharsets.UTF_8));
		assertThat(redirectUriParams.get("client_id")).containsExactly(UriUtils.encode(clientId, StandardCharsets.UTF_8));
		assertThat(redirectUriParams.get("redirect_uri")).containsExactly(UriUtils.encode(redirectUri, StandardCharsets.UTF_8));
		assertThat(redirectUriParams.get("scope")).containsExactly(UriUtils.encode(scope, StandardCharsets.UTF_8));
		assertThat(redirectUriParams.get("state")).containsExactly(UriUtils.encode(state, StandardCharsets.UTF_8));
	}

	@Test
	public void testEvaluate_openid4vpScheme() {
		server.addProperty("authorization_endpoint", "openid4vp://");

		authorizationEndpointRequest = new JsonObject();
		authorizationEndpointRequest.addProperty("client_id", clientId);
		authorizationEndpointRequest.addProperty("response_type", "vp_token");
		authorizationEndpointRequest.addProperty("nonce", "abc123");

		env.putObject("authorization_endpoint_request", authorizationEndpointRequest);

		cond.execute(env);

		String redirect = env.getString("redirect_to_authorization_endpoint");
		assertThat(redirect).startsWith("openid4vp://");
		assertThat(redirect).contains("client_id=" + clientId);
		assertThat(redirect).contains("response_type=vp_token");
		assertThat(redirect).contains("nonce=abc123");
	}

	@Test
	public void testEscape() throws UnsupportedEncodingException {
		authorizationEndpointRequest = new JsonObject();
		authorizationEndpointRequest.addProperty("state", "x=y&z;foo bar+foo%20bar");
		env.putObject("authorization_endpoint_request", authorizationEndpointRequest);

		cond.execute(env);

		// a '+' in a form-urlencoded query means "space", so it must be sent as %2B
		assertThat(env.getString("redirect_to_authorization_endpoint")).isEqualTo("https://example.com/oauth/authorize?state=x%3Dy%26z%3Bfoo%20bar%2Bfoo%2520bar");
	}

	@Test
	public void testJsonValueWithPlusIsFormUrlEncoded() {
		server.addProperty("authorization_endpoint", "openid4vp://authorize");
		authorizationEndpointRequest = new JsonObject();
		JsonObject clientMetadata = JsonParser.parseString("{\"vp_formats_supported\":{\"dc+sd-jwt\":{}}}").getAsJsonObject();
		authorizationEndpointRequest.add("client_metadata", clientMetadata);
		env.putObject("authorization_endpoint_request", authorizationEndpointRequest);

		cond.execute(env);

		String redirect = env.getString("redirect_to_authorization_endpoint");
		assertThat(redirect).isEqualTo("openid4vp://authorize?client_metadata=%7B%22vp_formats_supported%22%3A%7B%22dc%2Bsd-jwt%22%3A%7B%7D%7D%7D");
		// a wallet decoding the query as application/x-www-form-urlencoded reads the original JSON back
		assertThat(URLDecoder.decode(redirect.substring(redirect.indexOf('=') + 1), StandardCharsets.UTF_8)).isEqualTo(clientMetadata.toString());
	}

	@Test
	public void testExistingEndpointQueryIsRetained() {
		server.addProperty("authorization_endpoint", "https://example.com/oauth/authorize?tenant=a%20b");
		authorizationEndpointRequest = new JsonObject();
		authorizationEndpointRequest.addProperty("client_id", clientId);
		env.putObject("authorization_endpoint_request", authorizationEndpointRequest);

		cond.execute(env);

		assertThat(env.getString("redirect_to_authorization_endpoint")).isEqualTo("https://example.com/oauth/authorize?tenant=a%20b&client_id=" + clientId);
	}

}
