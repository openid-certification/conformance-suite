package net.openid.conformance.condition;

import net.openid.conformance.testmodule.TestLockManager;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A Spring {@link ClientHttpRequestInterceptor} that releases the test module's lock before
 * making an outgoing HTTP call and reacquires it afterwards.
 *
 * This prevents deadlocks when the remote party calls back to this test's endpoints during
 * the outgoing call. Without this, the incoming HTTP handler would block trying to acquire
 * the lock that the test thread holds while waiting for the HTTP response.
 *
 * If the current thread does not hold the lock (e.g. during CREATED status or in unit tests),
 * the interceptor is a no-op pass-through.
 */
public class LockReleasingRequestInterceptor implements ClientHttpRequestInterceptor {

	private final TestLockManager lockManager;
	private final ReentrantLock lock;

	public LockReleasingRequestInterceptor(TestLockManager lockManager, ReentrantLock lock) {
		this.lockManager = lockManager;
		this.lock = lock;
	}

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body,
			ClientHttpRequestExecution execution) throws IOException {
		if (lock.isHeldByCurrentThread()) {
			lockManager.releaseLock();
			try {
				return execution.execute(request, body);
			} finally {
				lockManager.reacquireLock();
			}
		} else {
			return execution.execute(request, body);
		}
	}
}
