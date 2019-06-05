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
public class ExpectExpiredTokenErrorFromTokenEndpoint_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ExpectExpiredTokenErrorFromTokenEndpoint cond;

	private JsonObject tokenEndpointResponse;

	@Before
	public void setUp() throws Exception {
		cond = new ExpectExpiredTokenErrorFromTokenEndpoint();

		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		tokenEndpointResponse = new JsonObject();

		env.putObject("token_endpoint_response", tokenEndpointResponse);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_NoErrorField() {
		cond.evaluate(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_ErrorFieldNotCorrect() {
		tokenEndpointResponse.addProperty("error", "access_denied");
		cond.evaluate(env);
	}

	@Test
	public void testEvaluate_ErrorFieldValid() {
		tokenEndpointResponse.addProperty("error", "expired_token");
		cond.evaluate(env);
	}
}
