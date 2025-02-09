package net.openid.conformance.condition.client;

import net.openid.conformance.condition.Condition;
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
public class ValidateVpTokenIsUnpaddedBase64Url_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ValidateVpTokenIsUnpaddedBase64Url cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new ValidateVpTokenIsUnpaddedBase64Url();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_isGood() {
		env.putString("vp_token", "abc");

		cond.execute(env);
	}

	@Test
	public void testEvaluate_equals() {
		assertThrows(ConditionError.class, () -> {
			env.putString("vp_token", "abc=");

			cond.execute(env);
		});
	}

}
