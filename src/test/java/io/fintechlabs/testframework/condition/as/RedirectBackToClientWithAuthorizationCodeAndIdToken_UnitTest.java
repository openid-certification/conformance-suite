package io.fintechlabs.testframework.condition.as;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import static org.junit.Assert.assertEquals;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;



@RunWith(MockitoJUnitRunner.class)
public class RedirectBackToClientWithAuthorizationCodeAndIdToken_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject params;

	private String redirectUri = "http://localhost:44444/";

	private String state = "test-state";

	private String hashState = "###test-state";

	private String idToken = "ABC123DEF456GHI789";

	private String code = "R5FTOpM0Rg";

	private JsonObject authorizationEndpointRequest;

	private RedirectBackToClientWithAuthorizationCodeAndIdToken cond;

	@Before
	public void setUp() throws Exception {
		cond = new RedirectBackToClientWithAuthorizationCodeAndIdToken();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate() {

		params = new JsonObject();
		params.addProperty("redirect_uri", redirectUri);
		params.addProperty("state", state);

		authorizationEndpointRequest = new JsonObject();
		authorizationEndpointRequest.add("params", params);

		env.putObject("authorization_endpoint_request", authorizationEndpointRequest);

		env.putString("id_token", idToken);
		env.putString("authorization_code", code);

		cond.evaluate(env);

		verify(env, atLeastOnce()).getString("id_token");
		verify(env, atLeastOnce()).getString("authorization_code");
		verify(env, atLeastOnce()).getObject("authorization_endpoint_request");

		assertThat(env.getString("authorization_endpoint_response_redirect")).startsWith(redirectUri);

		String redirectUri = (env.getString("authorization_endpoint_response_redirect"));

		String expectedString = ("http://localhost:44444/#state=test-state&code=R5FTOpM0Rg&id_token=ABC123DEF456GHI789");

		assertEquals(redirectUri, expectedString);
	}

	@Test
	public void testEvaluate_Encoding() {

		params = new JsonObject();
		params.addProperty("redirect_uri", redirectUri);
		params.addProperty("state", hashState);

		authorizationEndpointRequest = new JsonObject();
		authorizationEndpointRequest.add("params", params);

		env.putObject("authorization_endpoint_request", authorizationEndpointRequest);

		env.putString("id_token", idToken);
		env.putString("authorization_code", code);

		cond.evaluate(env);

		assertThat(env.getString("authorization_endpoint_response_redirect")).startsWith(redirectUri);

		String redirectUri = (env.getString("authorization_endpoint_response_redirect"));

		String expectedString = ("http://localhost:44444/#state=%23%23%23test-state&code=R5FTOpM0Rg&id_token=ABC123DEF456GHI789");

		assertEquals(redirectUri, expectedString);

	}
}
