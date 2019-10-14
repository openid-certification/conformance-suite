package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EnsureAuthorizationEndpointError_UnitTest {
	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject errorParams;

	private JsonObject successParams;

	private EnsureAuthorizationEndpointError cond;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new EnsureAuthorizationEndpointError();

		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

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
	 * Test method for {@link EnsureAuthorizationEndpointError#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_noError() {
		// This condition is looking for an error to pass
		env.putObject("authorization_endpoint_response", errorParams);
		cond.execute(env);
	}

	/**
	 * Test method for {@link CheckIfAuthorizationEndpointError#evaluate(Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_error() {
		// Looking for an error, so a successful callback will throw an error.
		env.putObject("authorization_endpoint_response", successParams);

		cond.execute(env);
	}
}
