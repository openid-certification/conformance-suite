package net.openid.conformance.condition.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class CreateRandomStateValue_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CreateRandomStateValue cond;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		cond = new CreateRandomStateValue();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	/**
	 * Test method for {@link CreateRandomStateValue#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate() {
		cond.execute(env);

		String res1 = env.getString("state");

		assertThat(res1).isNotNull();
		assertThat(res1).isNotEmpty();

		// call it twice to make sure we get a different value
		cond.execute(env);

		String res2 = env.getString("state");

		assertThat(res2).isNotEmpty();
		assertThat(res1).isNotEqualTo(res2);
	}
}
