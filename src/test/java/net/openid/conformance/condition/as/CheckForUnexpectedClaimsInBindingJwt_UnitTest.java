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

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class CheckForUnexpectedClaimsInBindingJwt_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CheckForUnexpectedClaimsInBindingJwt cond;

	@BeforeEach
	public void setUp() throws Exception {

		cond = new CheckForUnexpectedClaimsInBindingJwt();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_noErrors() {

		String claims = """
			{
				"sd_hash": "ukusTT_9zWsfkr8vASfbjusnl2US6u-hnO1U6QAkObI",
				"aud": "localhost.emobix.co.uk",
				"iat": 1732488192,
				"nonce": "pI8Dt80Ussos-._~"
			}
			""";

		env.putObjectFromJsonString("sdjwt", "binding.claims", claims);

		cond.execute(env);

	}

	/**
	 * Test method for {@link CheckForUnexpectedClaimsInRequestObject#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_misspeltClaim() {
		assertThrows(ConditionError.class, () -> {
			// misspelt sd_hash
			String claims = """
			{
				"sdhash": "ukusTT_9zWsfkr8vASfbjusnl2US6u-hnO1U6QAkObI",
				"aud": "localhost.emobix.co.uk",
				"iat": 1732488192,
				"nonce": "pI8Dt80Ussos-._~"
			}
			""";

			env.putObjectFromJsonString("sdjwt", "binding.claims", claims);

			cond.execute(env);
		});
	}

}
