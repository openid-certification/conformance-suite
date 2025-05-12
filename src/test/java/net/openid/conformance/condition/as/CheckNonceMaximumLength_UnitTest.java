package net.openid.conformance.condition.as;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class CheckNonceMaximumLength_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CheckNonceMaximumLength cond;


	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	public void setUp() throws Exception {

		cond = new CheckNonceMaximumLength();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	/**
	 * Test method for {@link  CheckNonceMaximumLength#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_noErrors() {
		env.putString("nonce", RandomStringUtils.secure().nextAlphabetic(43));

		cond.execute(env);
	}

	/**
	 * Test method for {@link  CheckNonceMaximumLength#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_invalidLength() {
		assertThrows(ConditionError.class, () -> {
			// The state shout not exceed 43 characters in length.
			env.putString("nonce", RandomStringUtils.secure().nextAlphabetic(44));

			cond.execute(env);
		});
	}
}
