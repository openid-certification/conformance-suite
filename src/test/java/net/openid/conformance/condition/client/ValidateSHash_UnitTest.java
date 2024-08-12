package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ValidateSHash_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ValidateSHash cond;

	/*
	 * @throws java.lang.Exception
	 */
	@BeforeEach
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
	 * Test method for {@link ValidateSHash#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_noError() {

		env.putString("state", "12345");
		addStateHash(env, "HS256", "WZRHGrsBESr8wYFZ9sx0tA");

		cond.execute(env);

	}

	public void testEvaluate_specexample_noError() {

		// This is the c_hash example from:
		// http://openid.net/specs/openid-connect-core-1_0.html#code-id_tokenExample
		// (the c_hash and s_hash algorithms are the same)
		env.putString("state", "Qcb0Orv1zh30vL1MPRsbm-diHiMwcLyZvn1arpZv-Jxf_11jnpEX3Tgfvk");
		addStateHash(env, "HS256", "LDktKdoQak3Pk0cnXxCltA");

		cond.execute(env);

		verify(env, atLeastOnce()).getString("s_hash", "s_hash");
		verify(env, atLeastOnce()).getString("state");
		verify(env, atLeastOnce()).getString("s_hash", "alg");
	}

	/**
	 * Test method for {@link ValidateSHash#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_missingIdToken() {
		assertThrows(ConditionError.class, () -> {

			env.putString("state", "12345");

			cond.execute(env);
		});
	}

	/**
	 * Test method for {@link ValidateSHash#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_missingHash() {
		assertThrows(ConditionError.class, () -> {

			env.putString("state", "12345");
			addStateHash(env, "HS256", null);

			cond.execute(env);
		});
	}

	/**
	 * Test method for {@link ValidateSHash#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_missingState() {
		assertThrows(ConditionError.class, () -> {

			addStateHash(env, "HS256", "WZRHGrsBESr8wYFZ9sx0tA");

			cond.execute(env);
		});
	}

	/**
	 * Test method for {@link ValidateSHash#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_missingAlg() {
		assertThrows(ConditionError.class, () -> {

			env.putString("state", "12345");
			addStateHash(env, null, "WZRHGrsBESr8wYFZ9sx0tA");

			cond.execute(env);
		});
	}

	/**
	 * Test method for {@link ValidateSHash#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_invalidAlg() {
		assertThrows(ConditionError.class, () -> {

			env.putString("state", "12345");
			addStateHash(env, "XXX", "WZRHGrsBESr8wYFZ9sx0tA");

			cond.execute(env);
		});
	}

	/**
	 * Test method for {@link ValidateSHash#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_badHash() {
		assertThrows(ConditionError.class, () -> {

			env.putString("state", "abcde");
			addStateHash(env, "HS256", "WZRHGrsBESr8wYFZ9sx0tA");

			cond.execute(env);
		});
	}

}
