package io.fintechlabs.testframework.condition.as;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;


@RunWith(MockitoJUnitRunner.class)
public class CalculateAtHash_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CalculateAtHash cond;

	private String signing_algorithm = "HS256";

	private String access_token = "jHkWEdUXMU1BwAsC4vtUsZwnNvTIxEl0z9K3vx5KF0Y";

	@Before
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

	@Test (expected = ConditionError.class)
	public void testEvaluate_InvalidAlgorithm()
	{
		env.putString("signing_algorithm", "ZZ256");

		env.putString("access_token", access_token);

		cond.execute(env);
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
