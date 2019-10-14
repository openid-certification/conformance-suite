package net.openid.conformance.condition.client;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
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
		cond.execute(env);
		assertThat(env.getString("times_server_called_notification_endpoint")).isEqualTo("1");
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_ServerCalledNotificationEndpointTwice() {
		env.putString("times_server_called_notification_endpoint", "1");
		cond.execute(env);
	}
}
