package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CheckStateInAuthorizationResponse_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CheckStateInAuthorizationResponse cond;

	private JsonObject responseWithState;

	private JsonObject responseWithoutState;

	@Before
	public void setUp() throws Exception {

		cond = new CheckStateInAuthorizationResponse();

		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		responseWithState = new JsonParser().parse("{"
			+ "\"code\":\"SplxlOBeZQQYbYS6WxSbIA\","
			+ "\"state\":\"xyz\""
			+ "}").getAsJsonObject();

		responseWithoutState = new JsonParser().parse("{"
			+ "\"code\":\"SplxlOBeZQQYbYS6WxSbIA\""
			+ "}").getAsJsonObject();
	}

	@Test
	public void testEvaluate_goodStateResponse() {

		env.putString("state", "xyz");
		env.putObject("authorization_endpoint_response", responseWithState);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_missingStateRequestAndResponse() {

		env.putObject("authorization_endpoint_response", responseWithoutState);

		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_missingStateResponse() {

		env.putString("state", "xyz");
		env.putObject("authorization_endpoint_response", responseWithoutState);

		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_wrongStateResponse() {

		env.putString("state", "abc_xyz");
		env.putObject("authorization_endpoint_response", responseWithState);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_missingStateResponseWithInvalidRequestObject() {

		env.putString("state", "xyz");
		env.putObject("authorization_endpoint_response", new JsonParser().parse("{\"error_description\": \"Invalid request parameter JWS\",\"error\": \"invalid_request_object\"}").getAsJsonObject());

		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_wrongStateResponseWithInvalidRequestObject() {

		env.putString("state", "abc_xyz");
		env.putObject("authorization_endpoint_response", new JsonParser().parse("{\"error_description\": \"Invalid request parameter JWS\",\"error\": \"invalid_request_object\",\"state\":\"xyz\"}").getAsJsonObject());

		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_missingStateResponseWithInvalidRequest() {

		env.putString("state", "xyz");
		env.putObject("authorization_endpoint_response", new JsonParser().parse("{\"error_description\": \"Invalid request parameter\",\"error\": \"invalid_request\"}").getAsJsonObject());

		cond.execute(env);
	}

	@Test
	public void testEvaluate_goodStateResponseWithInvalidRequest() {

		env.putString("state", "xyz");
		env.putObject("authorization_endpoint_response", new JsonParser().parse("{\"error_description\": \"Invalid request parameter\",\"error\": \"invalid_request\",\"state\":\"xyz\"}").getAsJsonObject());

		cond.execute(env);
	}
}
