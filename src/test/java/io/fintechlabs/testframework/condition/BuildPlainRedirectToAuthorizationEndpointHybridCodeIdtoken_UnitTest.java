package io.fintechlabs.testframework.condition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.fintechlabs.testframework.logging.EventLog;
import io.fintechlabs.testframework.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class BuildPlainRedirectToAuthorizationEndpointHybridCodeIdtoken_UnitTest {
	
	@Spy
	private Environment env = new Environment();
	
	@Mock
	private EventLog eventLog;
	
	private JsonObject server;
	
	private JsonObject client;
	
	private BuildPlainRedirectToAuthorizationEndpointHybridCodeIdtoken cond;
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		
		cond = new BuildPlainRedirectToAuthorizationEndpointHybridCodeIdtoken("UNIT-TEST", eventLog, false);
		
		server = new JsonParser().parse("{"
				+ "\"authorization_endpoint\":\"https://example.com/oauth/authorize\","
				+ "\"token_endpoint\":\"https://example.com/api/oauth/token\","
				+ "\"issuer\":\"ExampleApp\""
				+ "}").getAsJsonObject();
		
		client = new JsonParser().parse("{"
				+ "\"scope\":\"address phone openid email profile\""
				+ "}").getAsJsonObject();
		
		env.put("server", server);
		env.put("client", client);
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.BuildPlainRedirectToAuthorizationEndpointImplicit#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void testEvaluate() {
		
		env.putString("client_id", "s6BhdRkqt3");
		env.putString("state", "xyz");
		env.putString("redirect_uri", "https://client.example.com/cb");
		
		cond.evaluate(env);
		
		verify(env, atLeastOnce()).getString("server", "authorization_endpoint");
		verify(env, atLeastOnce()).getString("client_id");
		verify(env, atLeastOnce()).getString("state");
		verify(env, atLeastOnce()).getString("redirect_uri");
		verify(env, atLeastOnce()).getString("client", "scope");

		assertThat(env.getString("redirect_to_authorization_endpoint")).startsWith("https://example.com/oauth/authorize");
		
		UriComponents redirectUriComponents = UriComponentsBuilder.fromUriString(env.getString("redirect_to_authorization_endpoint")).build();
		Map<String, List<String>> redirectUriParams = redirectUriComponents.getQueryParams();

		assertThat(redirectUriParams.get("response_type")).containsExactly("code id_token");
		assertThat(redirectUriParams.get("client_id")).containsExactly("s6BhdRkqt3");
		assertThat(redirectUriParams.get("redirect_uri")).containsExactly("https://client.example.com/cb");
		assertThat(redirectUriParams.get("scope")).containsExactly("address phone openid email profile");
		assertThat(redirectUriParams.get("state")).containsExactly("xyz");
	}
}
