package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
public class CheckErrorFromBackchannelAuthenticationEndpointErrorInvalidRequest_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CheckErrorFromBackchannelAuthenticationEndpointErrorInvalidRequest cond;

	@Before
	public void setUp() throws Exception {

		cond = new CheckErrorFromBackchannelAuthenticationEndpointErrorInvalidRequest("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

	}

	@Test
	public void testEvaluate_caseGood() {
		JsonObject response = new JsonParser().parse("{\"error_description\":\"[A167303] A request object included in a backchannel authentication request must be signed.\",\"error\":\"invalid_request\",\"error_uri\":\"https://www.authlete.com/documents/apis/result_codes#A167303\"}").getAsJsonObject();

		env.putObject("backchannel_authentication_endpoint_response", response);

		cond.evaluate(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_caseWrongError() {
		JsonObject response = new JsonParser().parse("{\"error_description\":\"[A167303] A request object included in a backchannel authentication request must be signed.\",\"error\":\"invalid_client\",\"error_uri\":\"https://www.authlete.com/documents/apis/result_codes#A167303\"}").getAsJsonObject();

		env.putObject("backchannel_authentication_endpoint_response", response);

		cond.evaluate(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_caseErrorEmpty() {
		env.putObject("backchannel_authentication_endpoint_response", new JsonObject());

		cond.evaluate(env);
	}

}
