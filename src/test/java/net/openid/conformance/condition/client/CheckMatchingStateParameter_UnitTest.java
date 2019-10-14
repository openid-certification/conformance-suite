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
	 * Test method for {@link CheckMatchingStateParameter#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_withState_statePresent() {

		env.putString("state", "xyz");
		env.putObject("authorization_endpoint_response", paramsWithState);

		cond.execute(env);

		verify(env, atLeastOnce()).getString("state");
		verify(env, atLeastOnce()).getString("authorization_endpoint_response", "state");
	}

	/**
	 * Test method for {@link CheckMatchingStateParameter#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_withoutState_stateAbsent() {

		env.putObject("authorization_endpoint_response", paramsWithoutState);

		cond.execute(env);

		verify(env, atLeastOnce()).getString("state");
		verify(env, atLeastOnce()).getString("authorization_endpoint_response", "state");
	}

	/**
	 * Test method for {@link CheckMatchingStateParameter#evaluate(Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_withState_stateMissing() {

		env.putString("state", "xyz");
		env.putObject("authorization_endpoint_response", paramsWithoutState);

		cond.execute(env);
	}

	/**
	 * Test method for {@link CheckMatchingStateParameter#evaluate(Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_withState_stateMismatch() {

		env.putString("state", "abc");
		env.putObject("authorization_endpoint_response", paramsWithState);

		cond.execute(env);
	}

	/**
	 * Test method for {@link CheckMatchingStateParameter#evaluate(Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_withoutState_present() {

		env.putObject("authorization_endpoint_response", paramsWithState);

		cond.execute(env);
	}
}
