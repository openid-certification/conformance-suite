package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class ExpectServerDoesNotCallNotificationEndpointTwice_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ExpectServerDoesNotCallNotificationEndpointTwice cond;

	@Before
	public void setUp() throws Exception {
		cond = new ExpectServerDoesNotCallNotificationEndpointTwice();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate() {
		cond.evaluate(env);
		assertThat(env.getString("times_server_called_notification_endpoint")).isEqualTo("1");
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_ServerCalledNotificationEndpointTwice() {
		env.putString("times_server_called_notification_endpoint", "1");
		cond.evaluate(env);
	}
}
