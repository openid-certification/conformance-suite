package net.openid.conformance.testmodule;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Regression test for https://gitlab.com/openid/conformance-suite/-/work_items/1827: acquiring the
 * test lock must time out and fail rather than block the calling thread forever when another thread
 * holds the lock (e.g. a worker stuck in a long-running operation). Blocking here on the HTTP request
 * thread is what tied up workers and eventually brought the whole suite down.
 */
public class AbstractTestModuleLockTimeout_UnitTest {

	private static final long TEST_LOCK_TIMEOUT_SECONDS = 1;

	private static class LockTimeoutTestModule extends AbstractTestModule {
		@Override
		public void configure(JsonObject config, String baseUrl, String externalUrlOverride, String baseMtlsUrl) {
		}

		@Override
		public void start() {
		}

		@Override
		protected long getLockAcquireTimeoutSeconds() {
			return TEST_LOCK_TIMEOUT_SECONDS;
		}

		ReentrantLock testLock() {
			return env.getLock();
		}

		void doAcquireLock() {
			acquireLock();
		}
	}

	@Test
	public void acquireLock_timesOutWhenHeldByAnotherThread() throws Exception {
		LockTimeoutTestModule module = new LockTimeoutTestModule();
		CountDownLatch locked = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);

		Thread holder = new Thread(() -> {
			module.testLock().lock();
			locked.countDown();
			try {
				release.await(10, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} finally {
				module.testLock().unlock();
			}
		}, "lock-holder");
		holder.start();

		try {
			assertThat(locked.await(2, TimeUnit.SECONDS)).isTrue();

			Instant start = Instant.now();
			assertThrows(TestFailureException.class, module::doAcquireLock);
			Duration elapsed = Duration.between(start, Instant.now());

			// Should give up at around the configured timeout: it actually waited (not an instant
			// failure) and gave up well before any "hang".
			assertThat(elapsed).isGreaterThanOrEqualTo(Duration.ofMillis(500));
			assertThat(elapsed).isLessThan(Duration.ofSeconds(TEST_LOCK_TIMEOUT_SECONDS + 8L));
		} finally {
			release.countDown();
			holder.join(5000);
		}
	}
}
