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
public class CalculateCHash_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CalculateCHash cond;

	private String signing_algorithm = "HS256";

	private String authorizationCode = "Qcb0Orv1zh30vL1MPRsbm-diHiMwcLyZvn1arpZv-Jxf_11jnpEX3Tgfvk";

	@BeforeEach
	public void setUp() throws Exception {
		cond = new CalculateCHash();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO, new String[0]);
	}

	@Test
	public void testEvaluate_noError() {

		env.putString("signing_algorithm", signing_algorithm);

		env.putString("authorization_code", authorizationCode);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_InvalidAlgorithm()
	{
		assertThrows(ConditionError.class, () -> {
			env.putString("signing_algorithm", "ZZ256");

			env.putString("authorization_code", authorizationCode);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_specexample_noError() {

		// This is the c_hash example from:
		// http://openid.net/specs/openid-connect-core-1_0.html#code-id_tokenExample
		String expectedCHash = "LDktKdoQak3Pk0cnXxCltA";
		env.putString("authorization_code", "Qcb0Orv1zh30vL1MPRsbm-diHiMwcLyZvn1arpZv-Jxf_11jnpEX3Tgfvk");
		env.putString("signing_algorithm", "HS256");

		cond.execute(env);

		assertEquals(expectedCHash, env.getString("c_hash"));
		verify(env, atLeastOnce()).getString("authorization_code");
		verify(env, atLeastOnce()).getString("signing_algorithm");
	}
}
