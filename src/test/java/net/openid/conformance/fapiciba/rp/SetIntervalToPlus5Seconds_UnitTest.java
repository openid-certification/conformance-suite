package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.Condition;
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

@ExtendWith(MockitoExtension.class)
public class SetIntervalToPlus5Seconds_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private SetIntervalToPlus5Seconds condition;

	@BeforeEach
	public void setup() {
		condition = new SetIntervalToPlus5Seconds();
		condition.setProperties("test", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_noInterval() {
		assertThrows(NullPointerException.class, () -> {
			condition.evaluate(env);
		});
	}

	@Test
	public void testEvaluate_withInterval() {
		env.putInteger("interval", 10);

		condition.evaluate(env);

		assertEquals(Integer.valueOf(15), env.getInteger("interval"));
	}

}
