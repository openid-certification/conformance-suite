package io.fintechlabs.testframework.condition.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import com.google.gson.JsonObject;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
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
	@Before
	public void setUp() throws Exception {

		cond = new BuildPlainRedirectToAuthorizationEndpoint();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		server = new JsonObject();
		server.addProperty("authorization_endpoint", authorizationEndpoint);

		authorizationEndpointRequest = new JsonObject();
		authorizationEndpointRequest.addProperty("client_id", clientId);
		authorizationEndpointRequest.addProperty("redirect_uri", redirectUri);
		authorizationEndpointRequest.addProperty("scope", scope);
		authorizationEndpointRequest.addProperty("state", state);
		authorizationEndpointRequest.addProperty("response_type", responseType);

		env.putObject("server", server);
		env.putObject("authorization_endpoint_request", authorizationEndpointRequest);
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.BuildPlainRedirectToAuthorizationEndpoint#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 *
	 * @throws UnsupportedEncodingException
	 */
	@Test
	public void testEvaluate() throws UnsupportedEncodingException {

		env.putString("client_id", "s6BhdRkqt3");
		env.putString("state", "xyz");
		env.putString("redirect_uri", "https://client.example.com/cb");

		cond.evaluate(env);

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
}
