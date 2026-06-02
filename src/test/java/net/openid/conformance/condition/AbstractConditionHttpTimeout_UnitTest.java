package net.openid.conformance.condition;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.common.util.StalledHttpServer;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

/**
 * Regression test for https://gitlab.com/openid/conformance-suite/-/work_items/1827: an unresponsive
 * endpoint must make an outbound condition HTTP call fail within the configured timeout, rather than
 * hang the calling thread (the production timeout is 60s; the socket timeout alone is per-read and
 * does not bound the overall request, which is why a stalled endpoint hung a worker for hours).
 */
public class AbstractConditionHttpTimeout_UnitTest {

	private static final int TEST_TIMEOUT_SECONDS = 2;

	/** Condition whose outbound HTTP client uses a short timeout so the test runs quickly. */
	private static class TimeoutTestCondition extends AbstractCondition {
		@Override
		protected int getHttpClientTimeoutSeconds() {
			return TEST_TIMEOUT_SECONDS;
		}

		@Override
		public Environment evaluate(Environment env) {
			return env;
		}
	}

	private void assertFailsFast(StalledHttpServer.Mode mode) throws Exception {
		try (StalledHttpServer server = new StalledHttpServer(mode)) {
			TimeoutTestCondition cond = new TimeoutTestCondition();
			cond.setProperties("UNIT-TEST", mock(TestInstanceEventLog.class), ConditionResult.INFO);
			RestTemplate restTemplate = cond.createRestTemplate(new Environment());

			Instant start = Instant.now();
			assertThrows(ResourceAccessException.class,
				() -> restTemplate.getForObject(server.getUrl(), String.class));
			Duration elapsed = Duration.between(start, Instant.now());

			// It must give up at around the configured timeout: comfortably after it (so we know the
			// request actually waited and was not refused immediately) and well before any "hang".
			assertThat(elapsed).isGreaterThanOrEqualTo(Duration.ofSeconds(TEST_TIMEOUT_SECONDS - 1L));
			assertThat(elapsed).isLessThan(Duration.ofSeconds(TEST_TIMEOUT_SECONDS + 8L));
		}
	}

	@Test
	public void unresponsiveEndpoint_failsWithinTimeout() throws Exception {
		assertFailsFast(StalledHttpServer.Mode.ACCEPT_AND_HANG);
	}

	@Test
	public void endpointThatSendsHeadersThenStalls_failsWithinTimeout() throws Exception {
		assertFailsFast(StalledHttpServer.Mode.HEADERS_THEN_HANG);
	}
}
