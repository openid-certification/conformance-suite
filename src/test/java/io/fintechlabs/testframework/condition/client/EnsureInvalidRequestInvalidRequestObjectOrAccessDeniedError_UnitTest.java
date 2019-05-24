package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EnsureInvalidRequestInvalidRequestObjectOrAccessDeniedError_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private EnsureInvalidRequestInvalidRequestObjectOrAccessDeniedError cond;

	@Before
	public void setUp() throws Exception {

		cond = new EnsureInvalidRequestInvalidRequestObjectOrAccessDeniedError("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		JsonObject authorizationEndpointResponse = new JsonObject();

		authorizationEndpointResponse.addProperty("error", "invalid_request");

		authorizationEndpointResponse.addProperty("error_description", "[A167307] Failed to find a client application whose ID matches the value of the 'iss' claim in the request object included in the backchannel authentication request.");

		authorizationEndpointResponse.addProperty("error_uri", "https://www.authlete.com/documents/apis/result_codes#A167307");

		env.putObject("authorization_endpoint_response", authorizationEndpointResponse);

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_notExistErrorField() {

		JsonObject authorizationEndpointResponse = env.getObject("authorization_endpoint_response");

		authorizationEndpointResponse.remove("error");

		env.putObject("authorization_endpoint_response", authorizationEndpointResponse);

		cond.evaluate(env);

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_errorIsNotOneOfInvalidRequestInvalidRequestObjectOrAccessDenied() {

		JsonObject authorizationEndpointResponse = env.getObject("authorization_endpoint_response");

		authorizationEndpointResponse.addProperty("error", "slow_down");

		env.putObject("authorization_endpoint_response", authorizationEndpointResponse);

		cond.evaluate(env);

	}

	@Test
	public void testEvaluate_successWithInvalidRequestError() {
		cond.evaluate(env);
	}

	@Test
	public void testEvaluate_successWithInvalidRequestObjectError() {

		JsonObject authorizationEndpointResponse = env.getObject("authorization_endpoint_response");

		authorizationEndpointResponse.addProperty("error", "invalid_request_object");

		env.putObject("authorization_endpoint_response", authorizationEndpointResponse);

		cond.evaluate(env);

	}

	@Test
	public void testEvaluate_successWithAccessDeniedError() {

		JsonObject authorizationEndpointResponse = env.getObject("authorization_endpoint_response");

		authorizationEndpointResponse.addProperty("error", "access_denied");

		env.putObject("authorization_endpoint_response", authorizationEndpointResponse);

		cond.evaluate(env);

	}

}
