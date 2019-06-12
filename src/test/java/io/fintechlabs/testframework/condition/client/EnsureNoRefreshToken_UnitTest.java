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
public class EnsureNoRefreshToken_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject goodResponse;

	private JsonObject badResponse;

	private EnsureNoRefreshToken cond;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new EnsureNoRefreshToken();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		badResponse = new JsonParser().parse("{"
			+ "\"access_token\":\"2YotnFZFEjr1zCsicMWpAA\","
			+ "\"token_type\":\"example\","
			+ "\"expires_in\":3600,"
			+ "\"refresh_token\":\"tGzv3JOkF0XG5Qx2TlKWIA\","
			+ "\"example_parameter\":\"example_value\""
			+ "}").getAsJsonObject();

		goodResponse = new JsonParser().parse("{"
			+ "\"access_token\":\"2YotnFZFEjr1zCsicMWpAA\","
			+ "\"token_type\":\"example\","
			+ "\"expires_in\":3600,"
			+ "\"example_parameter\":\"example_value\""
			+ "}").getAsJsonObject();
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.CheckForRefreshTokenValue#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void testEvaluate_valuePresent() {

		env.putObject("token_endpoint_response", goodResponse);

		cond.evaluate(env);

		verify(env, atLeastOnce()).getString("token_endpoint_response", "refresh_token");
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.CheckForRefreshTokenValue#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_valueMissing() {

		env.putObject("token_endpoint_response", badResponse);

		cond.evaluate(env);
	}
}
