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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ExpectServerDoesNotCallNotificationEndpointTwice_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ExpectServerDoesNotCallNotificationEndpointTwice cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new ExpectServerDoesNotCallNotificationEndpointTwice();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate() {
		cond.execute(env);
		assertThat(env.getString("times_server_called_notification_endpoint")).isEqualTo("1");
	}

	@Test
	public void testEvaluate_ServerCalledNotificationEndpointTwice() {
		assertThrows(ConditionError.class, () -> {
			env.putString("times_server_called_notification_endpoint", "1");
			cond.execute(env);
		});
	}
}
