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
public class CheckForInvalidCharsInState_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CheckForInvalidCharsInState cond;


	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	public void setUp() throws Exception {

		cond = new CheckForInvalidCharsInState();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	/**
	 * Test method for {@link  CheckForInvalidCharsInState#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_noErrors() {
		env.putString(CreateEffectiveAuthorizationRequestParameters.ENV_KEY,
			CreateEffectiveAuthorizationRequestParameters.STATE, "aZB3-_.~");

		cond.execute(env);
	}

	/**
	 * Test method for {@link  CheckForInvalidCharsInState#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_invalidCharacters() {
		assertThrows(ConditionError.class, () -> {
			// The '^' and '!' character are not URL safe and thus invalid.
			env.putString(CreateEffectiveAuthorizationRequestParameters.ENV_KEY,
				CreateEffectiveAuthorizationRequestParameters.STATE, "aZB3-_.~^!");

			cond.execute(env);
		});
	}
}
