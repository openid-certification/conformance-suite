package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class BuildPlainRedirectToAuthorizationEndpoint_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

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

		assertThat(redirectUriParams.get("response_type")).containsExactly(UriUtils.encodeQueryParam(responseType, Charset.defaultCharset().name()));
		assertThat(redirectUriParams.get("client_id")).containsExactly(UriUtils.encodeQueryParam(clientId, Charset.defaultCharset().name()));
		assertThat(redirectUriParams.get("redirect_uri")).containsExactly(UriUtils.encodeQueryParam(redirectUri, Charset.defaultCharset().name()));
		assertThat(redirectUriParams.get("scope")).containsExactly(UriUtils.encodeQueryParam(scope, Charset.defaultCharset().name()));
		assertThat(redirectUriParams.get("state")).containsExactly(UriUtils.encodeQueryParam(state, Charset.defaultCharset().name()));
	}

	@Test
	public void testEscape() throws UnsupportedEncodingException {
		authorizationEndpointRequest = new JsonObject();
		authorizationEndpointRequest.addProperty("state", "x=y&z;foo bar+foo%20bar");
		env.putObject("authorization_endpoint_request", authorizationEndpointRequest);

		cond.execute(env);

		// This expected result is incorrect - the '+' should have been encoded to '%2B', as a + in the url
		// query means "space". The existing code doesn't handle that correctly, and I can't quickly find a way
		// to make Spring do the correct thing.
		// see https://github.com/spring-projects/spring-framework/issues/21577
		assertThat(env.getString("redirect_to_authorization_endpoint")).isEqualTo("https://example.com/oauth/authorize?state=x%3Dy%26z;foo%20bar+foo%2520bar");
	}

}
