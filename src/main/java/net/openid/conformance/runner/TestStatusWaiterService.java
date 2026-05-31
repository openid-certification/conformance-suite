package net.openid.conformance.runner;

import net.openid.conformance.testmodule.TestModule.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

/**
 * Per-testId registry of {@link Consumer} callbacks that fire when a test's
 * status transitions. Used by the long-poll endpoint
 * {@code GET /api/runner/{id}/wait-state} so the python test runner does not
 * have to poll a per-second status endpoint to discover transitions.
 *
 * <p>Thread-safety: all map mutations go through {@link ConcurrentHashMap#compute}
 * or {@link ConcurrentHashMap#remove}, both of which are atomic per key —
 * register/unregister/publish on the same testId serialise correctly without
 * a separate lock, and the map cannot retain empty queues.
 */
@Service
public class TestStatusWaiterService {

	private static final Logger log = LoggerFactory.getLogger(TestStatusWaiterService.class);

	private final Map<String, ConcurrentLinkedQueue<Consumer<Status>>> callbacksByTestId = new ConcurrentHashMap<>();

	/** Register a callback to be invoked when this test's status next changes.
	 *  Idempotent w.r.t. the same {@code callback} instance — registering the
	 *  same instance twice queues it twice (and it would fire twice on
	 *  publish). The caller is responsible for keeping a reference if it
	 *  wants to {@link #unregister} later. */
	public void register(String testId, Consumer<Status> callback) {
		callbacksByTestId.compute(testId, (k, queue) -> {
			ConcurrentLinkedQueue<Consumer<Status>> q = queue != null ? queue : new ConcurrentLinkedQueue<>();
			q.add(callback);
			return q;
		});
	}

	/** Remove a previously-registered callback. If this was the last callback
	 *  for the testId, the map entry is cleaned up. */
	public void unregister(String testId, Consumer<Status> callback) {
		callbacksByTestId.compute(testId, (k, queue) -> {
			if (queue == null) {
				return null;
			}
			queue.remove(callback);
			return queue.isEmpty() ? null : queue;
		});
	}

	/** Atomically remove and drain all callbacks for the given test, invoking
	 *  each with the new status. Concurrent registrations create a fresh
	 *  queue for the next publish (correct semantics: a callback that
	 *  registers after the publish starts waits for the next transition). */
	public void publishStatusChange(String testId, Status newStatus) {
		ConcurrentLinkedQueue<Consumer<Status>> queue = callbacksByTestId.remove(testId);
		if (queue == null) {
			return;
		}
		for (Consumer<Status> cb : queue) {
			try {
				cb.accept(newStatus);
			} catch (Exception e) {
				log.warn("status-change callback for {} threw", testId, e);
			}
		}
	}
}
