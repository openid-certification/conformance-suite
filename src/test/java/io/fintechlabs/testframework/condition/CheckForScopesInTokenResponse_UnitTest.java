package io.fintechlabs.testframework.condition;

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

import io.fintechlabs.testframework.logging.EventLog;
import io.fintechlabs.testframework.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class CheckForScopesInTokenResponse_UnitTest {
	
	@Spy
	private Environment env = new Environment();
	
	@Mock
	private EventLog eventLog;
	
	private JsonObject goodResponse;
	
	private JsonObject badResponse;
	
	private CheckForScopesInTokenResponse cond;
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		
		cond = new CheckForScopesInTokenResponse("UNIT-TEST", eventLog, false);
		
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
	 * Test method for {@link io.fintechlabs.testframework.condition.CheckForScopesInTokenResponse#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void testEvaluate_valuePresent() {
		
		env.put("token_endpoint_response", goodResponse);

		cond.evaluate(env);
		
		verify(env, atLeastOnce()).getString("token_endpoint_response", "scope");
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.CheckForScopesInTokenResponse#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_valueMissing() {
		
		env.put("token_endpoint_response", badResponse);

		cond.evaluate(env);
	}
}
