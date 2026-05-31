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
 * <p>Thread-safety: {@code register}/{@code unregister} mutate the map through
 * {@link ConcurrentHashMap#compute} (atomic per key); {@code publishStatusChange}
 * only reads. The map cannot retain empty queues because {@code unregister} removes
 * a testId's entry once its last callback is gone.
 */
@Service
public class TestStatusWaiterService {

	private static final Logger log = LoggerFactory.getLogger(TestStatusWaiterService.class);

	private final Map<String, ConcurrentLinkedQueue<Consumer<Status>>> callbacksByTestId = new ConcurrentHashMap<>();

	/** Register a callback to be invoked on every subsequent status change for this
	 *  test, until it is removed via {@link #unregister}. (The waiter does not know
	 *  which states the caller cares about — the callback itself filters and triggers
	 *  its {@link #unregister} when satisfied.) Registering the same instance twice
	 *  queues it twice; the caller must keep a reference to unregister later. */
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

	/** Invoke every registered callback for the given test with the new status.
	 *  Callbacks PERSIST across transitions — a waiter may be interested in a later
	 *  state, not merely the next one — so they are removed only by {@link #unregister}
	 *  (which the long-poll endpoint calls from the DeferredResult completion/timeout/
	 *  error callbacks), never here. A callback registered concurrently with this call
	 *  may miss the current transition (ConcurrentLinkedQueue iteration is weakly
	 *  consistent) but will see the next one; the endpoint's post-register status
	 *  re-read covers that window. */
	public void publishStatusChange(String testId, Status newStatus) {
		ConcurrentLinkedQueue<Consumer<Status>> queue = callbacksByTestId.get(testId);
		if (queue == null) {
			return;
		}
		for (Consumer<Status> cb : queue) {
			try {
				cb.accept(newStatus);
			} catch (Exception e) {
				// Deliberately swallow (do NOT rethrow). This runs on the test-execution thread
				// inside AbstractTestModule.setStatusInternal while the status lock is held. An
				// exception escaping here would propagate out of setStatusInternal into its
				// catch(Exception|Error), which only clears the lock and rethrows — corrupting the
				// status transition for the whole test because one waiter callback misbehaved. Log
				// and continue so the remaining waiters and the state machine are unaffected.
				log.warn("status-change callback for {} threw", testId, e);
			}
		}
	}
}
