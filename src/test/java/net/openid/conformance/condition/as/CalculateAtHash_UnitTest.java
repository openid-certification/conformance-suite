package net.openid.conformance.condition.as;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;


@ExtendWith(MockitoExtension.class)
public class CalculateAtHash_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CalculateAtHash cond;

	private String signing_algorithm = "HS256";

	private String access_token = "jHkWEdUXMU1BwAsC4vtUsZwnNvTIxEl0z9K3vx5KF0Y";

	@BeforeEach
	public void setUp() throws Exception {
		cond = new CalculateAtHash();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO, new String[0]);
	}

	@Test
	public void testEvaluate_noError() {

		env.putString("signing_algorithm", signing_algorithm);

		env.putString("access_token", access_token);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_InvalidAlgorithm()
	{
		assertThrows(ConditionError.class, () -> {
			env.putString("signing_algorithm", "ZZ256");

			env.putString("access_token", access_token);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_specexample_noError() {

		// This is the c_hash example from:
		//https://openid.net/specs/openid-connect-core-1_0.html#code-tokenExample
		String expectedAtHash = "77QmUPtjPfzWtF2AnpK9RQ";
		env.putString("access_token", "jHkWEdUXMU1BwAsC4vtUsZwnNvTIxEl0z9K3vx5KF0Y");
		env.putString("signing_algorithm", "HS256");

		cond.execute(env);

		assertEquals(expectedAtHash, env.getString("at_hash"));
		verify(env, atLeastOnce()).getString("access_token");
		verify(env, atLeastOnce()).getString("signing_algorithm");
	}
}
