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

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class CheckIfAuthorizationEndpointError_UnitTest {
	
	@Spy
	private Environment env = new Environment();
	
	@Mock
	private TestInstanceEventLog eventLog;
	
	private JsonObject errorParams;
	
	private JsonObject successParams;
	
	private CheckIfAuthorizationEndpointError cond;
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		
		cond = new CheckIfAuthorizationEndpointError("UNIT-TEST", eventLog, ConditionResult.INFO);
		
		successParams = new JsonParser().parse("{"
				+ "\"code\":\"SplxlOBeZQQYbYS6WxSbIA\","
				+ "\"state\":\"xyz\""
				+ "}").getAsJsonObject();
		
		errorParams = new JsonParser().parse("{"
				+ "\"error\":\"access_denied\","
				+ "\"state\":\"xyz\""
				+ "}").getAsJsonObject();
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.CheckIfAuthorizationEndpointError#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void testEvaluate_noError() {
		
		env.put("callback_params", successParams);

		cond.evaluate(env);
		
		verify(env, atLeastOnce()).getString("callback_params", "error");
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.CheckIfAuthorizationEndpointError#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_error() {
		
		env.put("callback_params", errorParams);

		cond.evaluate(env);
		
		verify(env, atLeastOnce()).getString("callback_params", "error");
	}
}
