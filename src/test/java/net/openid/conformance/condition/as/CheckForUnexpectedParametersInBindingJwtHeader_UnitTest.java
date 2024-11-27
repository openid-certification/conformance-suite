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
public class CheckForUnexpectedParametersInBindingJwtHeader_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CheckForUnexpectedParametersInBindingJwtHeader cond;

	@BeforeEach
	public void setUp() throws Exception {

		cond = new CheckForUnexpectedParametersInBindingJwtHeader();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_noErrors() {

		String header = """
			{
				"typ": "foo",
				"alg": "RS256"
			}
			""";

		env.putObjectFromJsonString("sdjwt", "binding.header", header);

		cond.execute(env);

	}

	/**
	 * Test method for {@link CheckForUnexpectedClaimsInRequestObject#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_misspeltClaim() {
		assertThrows(ConditionError.class, () -> {
			String header = """
			{
				"type": "foo",
				"alg": "RS256"
			}
			""";

			env.putObjectFromJsonString("sdjwt", "binding.header", header);

			cond.execute(env);
		});
	}

}
