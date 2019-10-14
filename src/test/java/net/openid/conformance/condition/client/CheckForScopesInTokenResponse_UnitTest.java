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
public class CheckForScopesInTokenResponse_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject goodResponse;

	private JsonObject badResponse;

	private CheckForScopesInTokenResponse cond;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new CheckForScopesInTokenResponse();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		goodResponse = new JsonParser().parse("{"
			+ "\"access_token\":\"MTQ0NjJkZmQ5OTM2NDE1ZTZjNGZmZjI3\","
			+ "\"token_type\":\"bearer\","
			+ "\"expires_in\":3600,"
			+ "\"refresh_token\":\"IwOGYzYTlmM2YxOTQ5MGE3YmNmMDFkNTVk\","
			+ "\"scope\":\"create\","
			+ "\"state\":\"12345678\""
			+ "}").getAsJsonObject();

		badResponse = new JsonParser().parse("{"
			+ "\"access_token\":\"MTQ0NjJkZmQ5OTM2NDE1ZTZjNGZmZjI3\","
			+ "\"token_type\":\"bearer\","
			+ "\"expires_in\":3600,"
			+ "\"refresh_token\":\"IwOGYzYTlmM2YxOTQ5MGE3YmNmMDFkNTVk\","
			+ "\"state\":\"12345678\""
			+ "}").getAsJsonObject();
	}

	/**
	 * Test method for {@link CheckForScopesInTokenResponse#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_valuePresent() {

		env.putObject("token_endpoint_response", goodResponse);

		cond.execute(env);

		verify(env, atLeastOnce()).getString("token_endpoint_response", "scope");
	}

	/**
	 * Test method for {@link CheckForScopesInTokenResponse#evaluate(Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_valueMissing() {

		env.putObject("token_endpoint_response", badResponse);

		cond.execute(env);
	}
}
