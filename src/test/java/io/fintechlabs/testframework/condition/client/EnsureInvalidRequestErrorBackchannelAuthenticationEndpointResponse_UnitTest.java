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
public class EnsureInvalidRequestErrorBackchannelAuthenticationEndpointResponse_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private EnsureInvalidRequestErrorBackchannelAuthenticationEndpointResponse cond;

	@Before
	public void setUp() throws Exception {
		cond = new EnsureInvalidRequestErrorBackchannelAuthenticationEndpointResponse("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		JsonObject backChannelAuthenticationEndpointResponse = new JsonObject();

		backChannelAuthenticationEndpointResponse.addProperty("error", "invalid_request");

		backChannelAuthenticationEndpointResponse.addProperty("error_description", "[A167307] Failed to find a client application whose ID matches the value of the 'iss' claim in the request object included in the backchannel authentication request.");

		backChannelAuthenticationEndpointResponse.addProperty("error_uri", "https://www.authlete.com/documents/apis/result_codes#A167307");

		env.putObject("backchannel_authentication_endpoint_response", backChannelAuthenticationEndpointResponse);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_notExistErrorField() {

		JsonObject backChannelAuthenticationEndpointResponse = env.getObject("backchannel_authentication_endpoint_response");

		backChannelAuthenticationEndpointResponse.remove("error");

		env.putObject("backchannel_authentication_endpoint_response", backChannelAuthenticationEndpointResponse);

		cond.evaluate(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_errorNotEqualInvalidRequest() {

		JsonObject backChannelAuthenticationEndpointResponse = env.getObject("backchannel_authentication_endpoint_response");

		backChannelAuthenticationEndpointResponse.addProperty("error", "slow_down");

		env.putObject("backchannel_authentication_endpoint_response", backChannelAuthenticationEndpointResponse);

		cond.evaluate(env);
	}

	@Test
	public void testEvaluate_success() {
		cond.evaluate(env);
	}

}
