package io.fintechlabs.testframework.condition.client;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.client.CheckIfTokenEndpointResponseError;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class CheckIfTokenEndpointResponseError_UnitTest {
	
	@Spy
	private Environment env = new Environment();
	
	@Mock
	private TestInstanceEventLog eventLog;
	
	private JsonObject successParams;
	
	private JsonObject errorParams;
	
	private CheckIfTokenEndpointResponseError cond;
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		
		cond = new CheckIfTokenEndpointResponseError("UNIT-TEST", eventLog, ConditionResult.INFO);
		
		successParams = new JsonParser().parse("{"
				+ "\"accessToken\":\"2YotnFZFEjr1zCsicMWpAA\","
				+ "\"token_type\":\"example\","
				+ "\"expires_in\":3600,"
				+ "\"refresh_token\":\"tGzv3JOkF0XG5Qx2TlKWIA\","
				+ "\"example_parameter\":\"example_value\""
				+ "}").getAsJsonObject();
		
		errorParams = new JsonParser().parse("{"
				+ "\"error\":\"invalid_request\""
				+ "}").getAsJsonObject();
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.CheckIfTokenEndpointReponseError#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void testEvaluate_noError() {
		
		env.put("token_endpoint_response", successParams);

		cond.evaluate(env);
		
		verify(env, atLeastOnce()).getString("token_endpoint_response", "error");
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.CheckIfTokenEndpointResponseError#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_error() {
		
		env.put("token_endpoint_response", errorParams);

		cond.evaluate(env);
		
		verify(env, atLeastOnce()).getString("token_endpoint_response", "error");
	}
}
