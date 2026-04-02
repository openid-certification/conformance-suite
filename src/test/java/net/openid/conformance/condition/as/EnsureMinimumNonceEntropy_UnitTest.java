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
public class EnsureMinimumNonceEntropy_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private EnsureMinimumNonceEntropy cond;

	@BeforeEach
	public void setUp() {
		cond = new EnsureMinimumNonceEntropy();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_highEntropyNonce() {
		env.putString("nonce", "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk");
		cond.execute(env);
	}

	@Test
	public void testEvaluate_lowEntropyNonce() {
		assertThrows(ConditionError.class, () -> {
			env.putString("nonce", "aaaa");
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_emptyNonce() {
		assertThrows(ConditionError.class, () -> {
			env.putString("nonce", "");
			cond.execute(env);
		});
	}
}
