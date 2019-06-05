package io.fintechlabs.testframework.condition.client;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.gson.JsonObject;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class ValidateSHash_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ValidateSHash cond;

	/*
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		cond = new ValidateSHash();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void addStateHash(Environment env, String alg, String s_hash) {

		JsonObject stateHash = new JsonObject();
		stateHash.addProperty("alg", alg);

		stateHash.addProperty("s_hash", s_hash);

		env.putObject("s_hash", stateHash);

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.ValidateSHash#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void testEvaluate_noError() {

		env.putString("state", "12345");
		addStateHash(env, "HS256", "WZRHGrsBESr8wYFZ9sx0tA");

		cond.evaluate(env);

	}

	public void testEvaluate_specexample_noError() {

		// This is the c_hash example from:
		// http://openid.net/specs/openid-connect-core-1_0.html#code-id_tokenExample
		// (the c_hash and s_hash algorithms are the same)
		env.putString("state", "Qcb0Orv1zh30vL1MPRsbm-diHiMwcLyZvn1arpZv-Jxf_11jnpEX3Tgfvk");
		addStateHash(env, "HS256", "LDktKdoQak3Pk0cnXxCltA");

		cond.evaluate(env);

		verify(env, atLeastOnce()).getString("s_hash", "s_hash");
		verify(env, atLeastOnce()).getString("state");
		verify(env, atLeastOnce()).getString("s_hash", "alg");
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.ValidateSHash#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingIdToken() {

		env.putString("state", "12345");

		cond.evaluate(env);
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.ValidateSHash#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingHash() {

		env.putString("state", "12345");
		addStateHash(env, "HS256", null);

		cond.evaluate(env);
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.ValidateSHash#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingState() {

		addStateHash(env, "HS256", "WZRHGrsBESr8wYFZ9sx0tA");

		cond.evaluate(env);
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.ValidateSHash#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingAlg() {

		env.putString("state", "12345");
		addStateHash(env, null, "WZRHGrsBESr8wYFZ9sx0tA");

		cond.evaluate(env);
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.ValidateSHash#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_invalidAlg() {

		env.putString("state", "12345");
		addStateHash(env, "XXX", "WZRHGrsBESr8wYFZ9sx0tA");

		cond.evaluate(env);
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.ValidateSHash#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_badHash() {

		env.putString("state", "abcde");
		addStateHash(env, "HS256", "WZRHGrsBESr8wYFZ9sx0tA");

		cond.evaluate(env);
	}

}
