package net.openid.conformance.condition.client;

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

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;

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

		cond = new CheckIfTokenEndpointResponseError();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

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

	@Test
	public void testEvaluate_noError() {

		env.putObject("token_endpoint_response", successParams);

		cond.execute(env);

		verify(env, atLeastOnce()).getString("token_endpoint_response", "error");
	}

	/**
	 * Test method for {@link CheckIfTokenEndpointResponseError#evaluate(Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_error() {

		env.putObject("token_endpoint_response", errorParams);

		cond.execute(env);

		verify(env, atLeastOnce()).getString("token_endpoint_response", "error");
	}
}
