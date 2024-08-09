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
public class CheckForInvalidCharsInNonce_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CheckForInvalidCharsInNonce cond;


	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	public void setUp() throws Exception {

		cond = new CheckForInvalidCharsInNonce();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	/**
	 * Test method for {@link  CheckForInvalidCharsInNonce#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_noErrors() {
		env.putString("nonce", "aZB3-_.~");
		cond.execute(env);
	}

	/**
	 * Test method for {@link  CheckForInvalidCharsInNonce#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_invalidCharacter() {
		assertThrows(ConditionError.class, () -> {
			// The '^' and '!' character are not URL safe and thus invalid.
			env.putString("nonce", "aZB3-_.~^!");
			cond.execute(env);
		});
	}
}
