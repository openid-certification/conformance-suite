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
public class ValidateErrorDescriptionFromTokenEndpointResponseError_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ValidateErrorDescriptionFromTokenEndpointResponseError cond;

	private JsonObject tokenEndpointResponse;

	@Before
	public void setUp() throws Exception {
		cond = new ValidateErrorDescriptionFromTokenEndpointResponseError();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		tokenEndpointResponse = new JsonParser().parse("{"
			+ "\"error_description\":\"[A200308] The end-user has not been authenticated yet.\","
			+ "\"error\":\"authorization_pending\","
			+ "\"error_uri\":\"https://www.authlete.com/documents/apis/result_codes#A200308\"}").getAsJsonObject();

		env.putObject("token_endpoint_response", tokenEndpointResponse);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_ErrorDescriptionFieldInvalid() {
		tokenEndpointResponse.addProperty("error_description", "[A200308] \"The end-user has not been authenticated yet.");
		cond.evaluate(env);
	}

	@Test
	public void testEvaluate_ErrorDescriptionFieldValid() {
		tokenEndpointResponse.addProperty("error_description", "[A200308] The end-user has not been authenticated yet.");
		cond.evaluate(env);
	}
}
