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
public class CheckMatchingStateParameter_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject paramsWithState;

	private JsonObject paramsWithoutState;

	private CheckMatchingStateParameter cond;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new CheckMatchingStateParameter();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		paramsWithState = new JsonParser().parse("{"
			+ "\"code\":\"SplxlOBeZQQYbYS6WxSbIA\","
			+ "\"state\":\"xyz\""
			+ "}").getAsJsonObject();

		paramsWithoutState = new JsonParser().parse("{"
			+ "\"code\":\"SplxlOBeZQQYbYS6WxSbIA\""
			+ "}").getAsJsonObject();
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.CheckMatchingStateParameter#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void testEvaluate_withState_statePresent() {

		env.putString("state", "xyz");
		env.putObject("callback_params", paramsWithState);

		cond.evaluate(env);

		verify(env, atLeastOnce()).getString("state");
		verify(env, atLeastOnce()).getString("callback_params", "state");
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.CheckMatchingStateParameter#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void testEvaluate_withoutState_stateAbsent() {

		env.putObject("callback_params", paramsWithoutState);

		cond.evaluate(env);

		verify(env, atLeastOnce()).getString("state");
		verify(env, atLeastOnce()).getString("callback_params", "state");
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.CheckMatchingStateParameter#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_withState_stateMissing() {

		env.putString("state", "xyz");
		env.putObject("callback_params", paramsWithoutState);

		cond.evaluate(env);
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.CheckMatchingStateParameter#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_withState_stateMismatch() {

		env.putString("state", "abc");
		env.putObject("callback_params", paramsWithState);

		cond.evaluate(env);
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.CheckMatchingStateParameter#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_withoutState_present() {

		env.putObject("callback_params", paramsWithState);

		cond.evaluate(env);
	}
}
