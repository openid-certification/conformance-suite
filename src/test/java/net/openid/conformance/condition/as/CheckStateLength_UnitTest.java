package net.openid.conformance.condition.as;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CheckStateLength_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CheckStateLength cond;


	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new CheckStateLength();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	/**
	 * Test method for {@link  CheckStateLength#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_noErrors() {
		env.putString(CreateEffectiveAuthorizationRequestParameters.ENV_KEY,
			CreateEffectiveAuthorizationRequestParameters.STATE, RandomStringUtils.randomAlphabetic(128));

		cond.execute(env);
	}

	/**
	 * Test method for {@link  CheckStateLength#evaluate(Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_invalidLength() {
		// The state shout not exceed 128 characters in length.
		env.putString(CreateEffectiveAuthorizationRequestParameters.ENV_KEY,
			CreateEffectiveAuthorizationRequestParameters.STATE, RandomStringUtils.randomAlphabetic(129));
		cond.execute(env);
	}
}
