package net.openid.conformance.condition.client;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class CreateLongRandomClientNotificationToken_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CreateLongRandomClientNotificationToken cond;

	/**
	 * @throws Exception
	 */
	@BeforeEach
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
