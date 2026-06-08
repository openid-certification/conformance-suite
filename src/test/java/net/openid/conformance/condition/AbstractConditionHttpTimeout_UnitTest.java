package net.openid.conformance.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.PooledConnectionManagers;
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
		assertFailsFast(mode, new Environment());
	}

	private void assertFailsFast(StalledHttpServer.Mode mode, Environment env) throws Exception {
		try (StalledHttpServer server = new StalledHttpServer(mode)) {
			TimeoutTestCondition cond = new TimeoutTestCondition();
			cond.setProperties("UNIT-TEST", mock(TestInstanceEventLog.class), ConditionResult.INFO);
			RestTemplate restTemplate = cond.createRestTemplate(env);

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

	/** An env that opts into connection pooling (same flag the metadata cache uses). */
	private static Environment poolingEnabledEnv() {
		JsonObject options = new JsonObject();
		options.addProperty("cache_external_metadata", true);
		JsonObject config = new JsonObject();
		config.add("options", options);
		Environment env = new Environment();
		env.putObject("config", config);
		return env;
	}

	/**
	 * The pooled path is the one the reviewer flagged as untested: there the per-request client shares a
	 * process-wide manager, so the deadline cannot abort by closing the client - it must evict that
	 * identity's pooled manager. SLOW_DRIP defeats the per-read socket timeout, so only the wall-clock
	 * deadline can stop it; if the pooled abort were wired wrong this would hang for ~2 minutes.
	 */
	@Test
	public void pooledRequestToDrippingEndpoint_failsWithinDeadline() throws Exception {
		PooledConnectionManagers.clear();
		try {
			assertFailsFast(StalledHttpServer.Mode.SLOW_DRIP, poolingEnabledEnv());
		} finally {
			PooledConnectionManagers.clear();
		}
	}

	@Test
	public void pooledRequestToUnresponsiveEndpoint_failsWithinDeadline() throws Exception {
		PooledConnectionManagers.clear();
		try {
			assertFailsFast(StalledHttpServer.Mode.ACCEPT_AND_HANG, poolingEnabledEnv());
		} finally {
			PooledConnectionManagers.clear();
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

	/**
	 * A peer that drips body bytes slower than a response cycle but faster than the per-read timeout
	 * defeats the socket/response timeout (which is per-read). Only the hard wall-clock deadline can
	 * stop it - without HttpRequestDeadlineInterceptor this call would run for ~2 minutes.
	 */
	@Test
	public void endpointThatDripsBytesUnderTheReadTimeout_failsWithinDeadline() throws Exception {
		assertFailsFast(StalledHttpServer.Mode.SLOW_DRIP);
	}
}
