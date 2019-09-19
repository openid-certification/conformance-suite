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
public class CheckErrorFromBackchannelAuthenticationEndpointErrorInvalidBindingMessage_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CheckErrorFromBackchannelAuthenticationEndpointErrorInvalidBindingMessage cond;

	@Before
	public void setUp() throws Exception {

		cond = new CheckErrorFromBackchannelAuthenticationEndpointErrorInvalidBindingMessage();

		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		JsonObject response = new JsonObject();

		response.addProperty("error", "invalid_binding_message");

		env.putObject("backchannel_authentication_endpoint_response", response);

	}

	@Test
	public void testEvaluate_caseGood() {
		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_caseErrorEmpty() {

		JsonObject response = env.getObject("backchannel_authentication_endpoint_response");

		response.remove("error");

		env.putObject("backchannel_authentication_endpoint_response", new JsonObject());

		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_caseWrongError() {

		JsonObject response = env.getObject("backchannel_authentication_endpoint_response");

		response.addProperty("error", "invalid_request");

		env.putObject("backchannel_authentication_endpoint_response", response);

		cond.execute(env);

	}

}
