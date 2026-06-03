package net.openid.conformance.logging;

import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.DataUtils;
import net.openid.conformance.testmodule.TestLockManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A Spring {@link ClientHttpRequestInterceptor} that logs outgoing HTTP requests and responses,
 * and optionally releases the test module's lock during network I/O to prevent deadlocks.
 *
 * <p>When a {@link TestLockManager} is provided, the interceptor:
 * <ol>
 *   <li>Logs the request (lock held, for deterministic log ordering)</li>
 *   <li>Releases the lock</li>
 *   <li>Executes the HTTP call and buffers the full response body</li>
 *   <li>Reacquires the lock</li>
 *   <li>Logs the response (lock held, for deterministic log ordering)</li>
 * </ol>
 *
 * <p>Buffering the response body while the lock is released is essential because Apache HttpClient
 * returns a streaming response — reading the body is still network I/O. Without this, a remote
 * party that sends headers and then calls back into the suite before completing the body would
 * deadlock on the lock.
 *
 * <p>If the current thread does not hold the lock (e.g. during CREATED status or in unit tests),
 * or no lock manager was provided, the interceptor simply logs and passes through.
 */
public class LoggingRequestInterceptor implements ClientHttpRequestInterceptor, DataUtils {

	private final String source;
	private final TestInstanceEventLog log;
	private final JsonObject mutualTls;
	private final TestLockManager lockManager;
	private final ReentrantLock lock;

	public LoggingRequestInterceptor(String source, TestInstanceEventLog log, JsonObject mutualTls) {
		this(source, log, mutualTls, null, null);
	}

	public LoggingRequestInterceptor(String source, TestInstanceEventLog log, JsonObject mutualTls,
			TestLockManager lockManager, ReentrantLock lock) {
		this.source = source;
		this.log = log;
		this.mutualTls = mutualTls;
		this.lockManager = lockManager;
		this.lock = lock;
	}

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
		// Log request while lock is held for deterministic ordering
		logRequest(request, body);

		WrappedClientHttpResponse response;
		if (lockManager != null && lock.isHeldByCurrentThread()) {
			lockManager.releaseLock();
			try {
				// All network I/O (HTTP call + response body read) happens with lock released
				response = new WrappedClientHttpResponse(execution.execute(request, body));
			} finally {
				lockManager.reacquireLock();
			}
		} else {
			if (lock != null && lock.isHeldByCurrentThread()) {
				// We hold the per-test lock but have no TestLockManager to release it for the duration of
				// the network I/O. This should never happen in production: every condition run by a test
				// module is given a lock manager (AbstractTestModule.call()). It is expected only in
				// standalone condition unit tests, which do not hold the lock - hence this branch is gated
				// on isHeldByCurrentThread(). If it ever happens with the lock held, this network call would
				// block every other thread that needs this test's lock (including stop()) for the duration
				// of the I/O and could hang the whole suite, so fail loudly instead of performing the I/O.
				// See https://gitlab.com/openid/conformance-suite/-/work_items/1827
				throw new IllegalStateException("HTTP I/O to " + request.getURI() + " while holding the test "
					+ "lock with no lock manager to release it (source=" + source + ") would block other "
					+ "threads waiting on this test's lock and could hang the suite. This is a bug in the test suite.");
			}
			response = new WrappedClientHttpResponse(execution.execute(request, body));
		}

		// Log response while lock is held for deterministic ordering
		logResponse(response);
		return response;
	}

	private void logRequest(HttpRequest request, byte[] body) throws IOException {
		JsonObject o = new JsonObject();
		o.addProperty("request_uri", request.getURI().toString());
		o.addProperty("request_method", request.getMethod().toString());
		o.add("request_headers", mapToJsonObject(request.getHeaders(), false));
		if (body != null) {
			o.addProperty("request_body", new String(body, StandardCharsets.UTF_8));
		}
		o.addProperty("msg", "HTTP request");
		o.addProperty("http", "request");
		if (mutualTls != null) {
			o.add("request_mutual_tls", mutualTls);
		}
		log.log(source, o);
	}

	private void logResponse(WrappedClientHttpResponse response) throws IOException {
		JsonObject o = new JsonObject();
		o.addProperty("response_status_code", response.getStatusCode().toString());
		o.addProperty("response_status_text", response.getStatusText());
		o.add("response_headers", mapToJsonObject(response.getHeaders(), true));
		if (response.body != null) {
			o.addProperty("response_body", new String(response.body, StandardCharsets.UTF_8));
		}
		if (response.cacheAgeSeconds != null) {
			o.addProperty("msg", "Using cached HTTP response");
			o.addProperty("cache_age_seconds", response.cacheAgeSeconds);
		} else {
			o.addProperty("msg", "HTTP response");
		}
		o.addProperty("http", "response");
		if (response.bodyException != null) {
			o.addProperty("exception_reading_body", response.bodyException.getMessage());
		}
		log.log(source, o);
	}

	private static final class WrappedClientHttpResponse implements ClientHttpResponse {

		private final ClientHttpResponse response;
		private byte[] body;
		private IOException bodyException;
		// null when the response came from the network; set to the cached
		// entry's age when an inner interceptor returned a replayed response
		// tagged with CachedHttpResponseMarker.
		private final Long cacheAgeSeconds;

		public WrappedClientHttpResponse(ClientHttpResponse response) {
			this.response = response;
			try {
				this.body = StreamUtils.copyToByteArray(response.getBody());
				this.bodyException = null;
			} catch (IOException e) {
				this.body = null;
				this.bodyException = e;
			}
			this.cacheAgeSeconds = (response instanceof CachedHttpResponseMarker marker)
				? marker.getCacheAgeSeconds() : null;
		}

		@Override
		public InputStream getBody() throws IOException {
			if (body != null) {
				return new ByteArrayInputStream(body);
			} else {
				throw bodyException;
			}
		}

		@Override
		public HttpHeaders getHeaders() {
			return response.getHeaders();
		}

		@Override
		public HttpStatusCode getStatusCode() throws IOException {
			return response.getStatusCode();
		}

		@Override
		public String getStatusText() throws IOException {
			return response.getStatusText();
		}

		@Override
		public void close() {
			response.close();
		}

	}

}
