package io.fintechlabs.testframework.condition.client;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class CheckIfAccountRequestsEndpointResponseError_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject successParams;

	private JsonObject errorParams;

	private CheckIfAccountRequestsEndpointResponseError cond;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new CheckIfAccountRequestsEndpointResponseError();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		successParams = new JsonObject();

		errorParams = new JsonParser().parse("{\"error\":\"access_denied\"}").getAsJsonObject();

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.CheckIfAccountRequestsEndpointResponseError#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void testEvaluate_noError() {

		env.putObject("account_requests_endpoint_response", successParams);

		cond.execute(env);

		verify(env, atLeastOnce()).getString("account_requests_endpoint_response", "error");
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.CheckIfAccountRequestsEndpointResponseError#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_error() {

		env.putObject("account_requests_endpoint_response", errorParams);

		cond.execute(env);

	}

}
