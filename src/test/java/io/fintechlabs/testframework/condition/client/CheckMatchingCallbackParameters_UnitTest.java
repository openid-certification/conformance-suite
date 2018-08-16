package io.fintechlabs.testframework.condition.client;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class CheckMatchingCallbackParameters_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private String uriWithoutSuffix;

	private String uriWithSuffix;

	private JsonObject goodParams;

	private JsonObject badParams;

	private CheckMatchingCallbackParameters cond;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new CheckMatchingCallbackParameters("UNIT-TEST", eventLog, ConditionResult.INFO);

		uriWithoutSuffix = "https://example.com/callback";

		uriWithSuffix = uriWithoutSuffix + "?dummy1=lorem&dummy2=ipsum";

		goodParams = new JsonParser().parse("{"
				+ "\"dummy1\":\"lorem\","
				+ "\"dummy2\":\"ipsum\""
				+ "}").getAsJsonObject();

		badParams = new JsonParser().parse("{"
				+ "\"dummy1\":\"dolor\""
				+ "}").getAsJsonObject();
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.CheckMatchingCallbackParameters#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void testEvaluate_noSuffix() {

		env.putString("redirect_uri", uriWithoutSuffix);
		env.putObject("callback_query_params", new JsonObject());

		cond.evaluate(env);

		verify(env, atLeastOnce()).getString("redirect_uri");
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.CheckMatchingCallbackParameters#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void testEvaluate_withSuffix_noError() {

		env.putString("redirect_uri", uriWithSuffix);
		env.putObject("callback_query_params", goodParams);

		cond.evaluate(env);

		verify(env, atLeastOnce()).getString("redirect_uri");
		verify(env, atLeastOnce()).getString("callback_query_params", "dummy1");
		verify(env, atLeastOnce()).getString("callback_query_params", "dummy2");
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.CheckMatchingCallbackParameters#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_withSuffix_badParams() {

		env.putString("redirect_uri", uriWithSuffix);
		env.putObject("callback_params", badParams);

		cond.evaluate(env);
	}

}
