package net.openid.conformance.logging;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Enforces a hard wall-clock deadline on an outbound HTTP request.
 *
 * <p>Apache HttpClient's socket and response timeouts are per-read inactivity timeouts, not a cap on
 * the whole request: a peer that keeps sending a few bytes before each timeout (or trickles a
 * response body) can hold the connection - and the calling thread - open indefinitely. This
 * interceptor schedules an abort action (closing the underlying HTTP client, which aborts the
 * in-flight request) if the whole call, <em>including response-body buffering</em>, exceeds the
 * deadline, so a single request can never pin a thread forever.
 *
 * <p>It must be registered as the outermost interceptor so that its window covers any response-body
 * reading done by inner interceptors. On normal completion the scheduled abort is cancelled, so the
 * client is only ever closed when the deadline is actually exceeded (i.e. when the request is failing
 * anyway). See https://gitlab.com/openid/conformance-suite/-/work_items/1827
 */
public class HttpRequestDeadlineInterceptor implements ClientHttpRequestInterceptor {

	private static final ScheduledThreadPoolExecutor SCHEDULER = createScheduler();

	private static ScheduledThreadPoolExecutor createScheduler() {
		ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(1, r -> {
			Thread t = new Thread(r, "http-request-deadline-watchdog");
			t.setDaemon(true);
			return t;
		});
		// The vast majority of requests complete before the deadline and cancel their abort task.
		// Remove cancelled tasks from the queue immediately so they don't accumulate (they would
		// otherwise linger until their delay elapsed, which on a busy server is a memory leak).
		scheduler.setRemoveOnCancelPolicy(true);
		return scheduler;
	}

	private final Runnable abortAction;
	private final long deadlineSeconds;

	public HttpRequestDeadlineInterceptor(Runnable abortAction, long deadlineSeconds) {
		this.abortAction = abortAction;
		this.deadlineSeconds = deadlineSeconds;
	}

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
		ScheduledFuture<?> abort = SCHEDULER.schedule(abortAction, deadlineSeconds, TimeUnit.SECONDS);
		try {
			return execution.execute(request, body);
		} finally {
			abort.cancel(false);
		}
	}
}
