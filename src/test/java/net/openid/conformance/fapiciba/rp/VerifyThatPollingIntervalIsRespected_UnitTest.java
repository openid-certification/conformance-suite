package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class VerifyThatPollingIntervalIsRespected_UnitTest {

	private final Environment env = new Environment();
	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();
	private VerifyThatPollingIntervalIsRespected condition;

	@BeforeEach
	public void setUp() {
		condition = new VerifyThatPollingIntervalIsRespected();
		condition.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		env.putInteger("interval", 5);
		env.putString("next_allowed_token_request",
			DateTimeFormatter.ISO_INSTANT.format(Instant.now().plusSeconds(60)));
	}

	@Test
	public void rejectsFallbackPollBeforeIntervalWhenPingWasNotAttempted() {
		assertThatThrownBy(() -> condition.execute(env)).isInstanceOf(ConditionError.class);
	}

	@Test
	public void permitsSynchronousTokenRedemptionAfterPingAttemptStarts() {
		env.putBoolean(PingClientNotificationEndpoint.CLIENT_PING_ATTEMPTED, true);

		assertThatNoException().isThrownBy(() -> condition.execute(env));
	}

	@Test
	public void legacyPingFlagDoesNotUnlockFallbackPolling() {
		env.putBoolean("client_was_pinged", true);

		assertThatThrownBy(() -> condition.execute(env)).isInstanceOf(ConditionError.class);
	}
}
