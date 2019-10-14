package net.openid.conformance.condition.client;

import net.openid.conformance.condition.Condition.ConditionResult;
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
public class CreateLongRandomClientNotificationToken_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CreateLongRandomClientNotificationToken cond;

	/**
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {
		cond = new CreateLongRandomClientNotificationToken();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate() {
		cond.execute(env);

		String res1 = env.getString("client_notification_token");

		assertThat(res1).isNotNull();
		assertThat(res1).isNotEmpty();
		assertThat(res1.length()).isEqualTo(1024);

		// call it twice to make sure we get a different value
		cond.execute(env);

		String res2 = env.getString("client_notification_token");

		assertThat(res2).isNotEmpty();
		assertThat(res1).isNotEqualTo(res2);
	}
}
