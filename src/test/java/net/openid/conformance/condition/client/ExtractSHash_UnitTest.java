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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ExtractSHash_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ExtractSHash cond;

	/*
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	public void setUp() throws Exception {
		cond = new ExtractSHash();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	private void addIdToken(Environment env, String alg, String stateHash) {

		JsonObject header = new JsonObject();

		if (alg != null) {
			header.addProperty("alg", alg);
		}

		JsonObject claims = new JsonObject();
		if (stateHash != null) {
			claims.addProperty("s_hash", stateHash);
		}

		JsonObject idToken = new JsonObject();
		idToken.add("header", header);
		idToken.add("claims", claims);

		env.putObject("id_token", idToken);

	}

	/**
	 * Test method for {@link ValidateSHash#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_noError() {

		env.putString("state", "12345");
		addIdToken(env, "HS256", "WZRHGrsBESr8wYFZ9sx0tA");

		cond.execute(env);

		verify(env, atLeastOnce()).getString("id_token", "claims.s_hash");
		verify(env, atLeastOnce()).getString("id_token", "header.alg");

		assertThat(env.getString("s_hash", "s_hash")).isEqualTo("WZRHGrsBESr8wYFZ9sx0tA");
		assertThat(env.getString("s_hash", "alg")).isEqualTo("HS256");

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
			addIdToken(env, "HS256", null);

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
			addIdToken(env, null, "WZRHGrsBESr8wYFZ9sx0tA");

			cond.execute(env);
		});
	}

}
