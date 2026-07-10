package net.openid.conformance.condition.client;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class EnsureNotificationEndpointWasRetried_UnitTest {

	private final Environment env = new Environment();
	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();
	private EnsureNotificationEndpointWasRetried condition;

	@BeforeEach
	public void setUp() throws Exception {
		condition = new EnsureNotificationEndpointWasRetried();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void acceptsSecondNotificationAttempt() {
		env.putInteger("notification_endpoint_call_count", 2);

		assertThatCode(() -> condition.execute(env)).doesNotThrowAnyException();
	}

	@Test
	public void rejectsOnlyOneNotificationAttempt() {
		env.putInteger("notification_endpoint_call_count", 1);

		assertThrows(ConditionError.class, () -> condition.execute(env));
	}
}
