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
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.junit.Assert.assertEquals;



@RunWith(MockitoJUnitRunner.class)
public class CalculateCHash_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CalculateCHash cond;

	private String signing_algorithm = "HS256";

	private String authorizationCode = "Qcb0Orv1zh30vL1MPRsbm-diHiMwcLyZvn1arpZv-Jxf_11jnpEX3Tgfvk";

	@Before
	public void setUp() throws Exception {
		cond = new CalculateCHash();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO, new String[0]);
	}

	@Test
	public void testEvaluate_noError() {

		env.putString("signing_algorithm", signing_algorithm);

		env.putString("authorization_code", authorizationCode);

		cond.evaluate(env);
	}

	@Test (expected = ConditionError.class)
	public void testEvaluate_InvalidAlgorithm()
	{
		env.putString("signing_algorithm", "ZZ256");

		cond.evaluate(env);
	}

	@Test
	public void testEvaluate_specexample_noError() {

		// This is the c_hash example from:
		// http://openid.net/specs/openid-connect-core-1_0.html#code-id_tokenExample
		String expectedCHash = "LDktKdoQak3Pk0cnXxCltA";
		env.putString("authorization_code", "Qcb0Orv1zh30vL1MPRsbm-diHiMwcLyZvn1arpZv-Jxf_11jnpEX3Tgfvk");
		env.putString("signing_algorithm", "HS256");

		cond.evaluate(env);

		assertEquals(expectedCHash, env.getString("c_hash"));
		verify(env, atLeastOnce()).getString("authorization_code");
		verify(env, atLeastOnce()).getString("signing_algorithm");
	}
}
