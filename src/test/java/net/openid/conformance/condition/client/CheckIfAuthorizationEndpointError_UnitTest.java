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

		cond = new CheckIfAuthorizationEndpointError();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

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
	 * Test method for {@link CheckIfAuthorizationEndpointError#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_noError() {

		env.putObject("authorization_endpoint_response", successParams);

		cond.execute(env);

		verify(env, atLeastOnce()).getString("authorization_endpoint_response", "error");
	}

	/**
	 * Test method for {@link CheckIfAuthorizationEndpointError#evaluate(Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_error() {

		env.putObject("authorization_endpoint_response", errorParams);

		cond.execute(env);

		verify(env, atLeastOnce()).getString("authorization_endpoint_response", "error");
	}
}
