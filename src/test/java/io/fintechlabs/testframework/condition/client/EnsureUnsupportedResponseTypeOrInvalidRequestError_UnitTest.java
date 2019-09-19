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
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EnsureUnsupportedResponseTypeOrInvalidRequestError_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private EnsureUnsupportedResponseTypeOrInvalidRequestError cond;

	@Before
	public void setUp() throws Exception {

		cond = new EnsureUnsupportedResponseTypeOrInvalidRequestError();

		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		JsonObject authorizationEndpointResponse = new JsonObject();

		authorizationEndpointResponse.addProperty("error", "unsupported_response_type");

		authorizationEndpointResponse.addProperty("error_description", "[A009305] The response type 'code' is not supported by this service.");

		authorizationEndpointResponse.addProperty("error_uri", "https://docs.authlete.com/#A009305");

		env.putObject("authorization_endpoint_response", authorizationEndpointResponse);

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_notExistErrorField() {

		JsonObject authorizationEndpointResponse = env.getObject("authorization_endpoint_response");

		authorizationEndpointResponse.remove("error");

		env.putObject("authorization_endpoint_response", authorizationEndpointResponse);

		cond.execute(env);

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_errorIsNotOneOfUnsupportedResponseTypeOrInvalidRequest() {

		JsonObject authorizationEndpointResponse = env.getObject("authorization_endpoint_response");

		authorizationEndpointResponse.addProperty("error", "access_denied");

		env.putObject("authorization_endpoint_response", authorizationEndpointResponse);

		cond.execute(env);

	}

	@Test
	public void testEvaluate_successWithErrorUnsupportedResponseType() {
		cond.execute(env);
	}

	@Test
	public void testEvaluate_successWithInvalidRequestError() {

		JsonObject authorizationEndpointResponse = env.getObject("authorization_endpoint_response");

		authorizationEndpointResponse.addProperty("error", "invalid_request");

		env.putObject("authorization_endpoint_response", authorizationEndpointResponse);

		cond.execute(env);

	}

}
