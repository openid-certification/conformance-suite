package net.openid.conformance.condition.as;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Process-wide, namespaced, per-scope history of recently seen values, used to detect a
 * value being reused across requests when each test module finishes after a single request
 * and per-module {@link net.openid.conformance.testmodule.Environment}s are isolated - so
 * reuse can only be observed across separate module instances / test runs.
 *
 * <p>Used to detect a verifier reusing a response-encryption key
 * ({@link VP1FinalCheckEncryptionKeyNotReused}); it could equally back other cross-request reuse
 * checks, e.g. a request-object {@code jti} reuse check in {@link ValidateRequestObjectJti} (which
 * does not currently use it). Each check picks a {@code namespace} (so unrelated checks
 * never collide) and a {@code scope} string (e.g. the logged-in suite user) within it. The test id
 * a value was first seen in is recorded too, so a reuse error can point at the earlier test.
 *
 * <p>Modeled on {@link net.openid.conformance.condition.client.ExternalEndpointCache}: a static
 * facade is the simplest way to share state across the reflectively-instantiated, non-Spring
 * Condition instances. Each (namespace, scope) keeps a bounded, time-windowed FIFO of values:
 * entries older than {@link #DEFAULT_RETENTION} are pruned and at most {@link #MAX_PER_SCOPE}
 * are kept (oldest dropped first). The window bounds memory and avoids comparing values across
 * unrelated sessions far apart in time.
 */
public final class RecentValueHistory {

	/** How long a seen value is remembered for reuse detection (a "few hours"). */
	static final Duration DEFAULT_RETENTION = Duration.ofHours(3);

	/** FIFO bound per (namespace, scope), to cap memory on a long-running server. */
	static final int MAX_PER_SCOPE = 100;

	private record Seen(String value, String testId, Instant at) { }

	/** A previously seen value and the test id it was first recorded in. */
	public record SeenValue(String value, String testId) { }

	// All access goes through the synchronized static methods, so no concurrent map is needed.
	private static final Map<String, Deque<Seen>> ENTRIES = new HashMap<>();
	private static final AtomicReference<Duration> retention = new AtomicReference<>(DEFAULT_RETENTION);

	private RecentValueHistory() { }

	private static String key(String namespace, String scope) {
		// Namespaces are fixed identifiers with no spaces, so the first space unambiguously
		// separates namespace from scope (the scope may itself contain spaces).
		return namespace + " " + scope;
	}

	/**
	 * Atomically check all {@code values} against the history for ({@code namespace}, {@code scope})
	 * and, if none have been seen within the retention window, record them all as seen now in test
	 * {@code testId}. Checking happens before any recording, so a single request that legitimately
	 * contains the same value twice does not self-trigger. When a value has been seen, nothing is
	 * recorded.
	 *
	 * @return the first previously-seen value (with the test id it was first seen in), or
	 * {@code null} if none were seen (in which case all values have been recorded)
	 */
	public static synchronized SeenValue checkAndRecord(String namespace, String scope, List<String> values, String testId) {
		for (String value : values) {
			String firstSeenTestId = firstSeenTestId(namespace, scope, value);
			if (firstSeenTestId != null) {
				return new SeenValue(value, firstSeenTestId);
			}
		}
		for (String value : values) {
			record(namespace, scope, value, testId);
		}
		return null;
	}

	/**
	 * @return the test id in which {@code value} was first recorded for ({@code namespace},
	 * {@code scope}) within the retention window, or {@code null} if it has not been seen. Prunes
	 * expired entries as a side effect; does NOT record.
	 */
	static synchronized String firstSeenTestId(String namespace, String scope, String value) {
		Deque<Seen> dq = ENTRIES.get(key(namespace, scope));
		if (dq == null) {
			return null;
		}
		prune(dq);
		for (Seen s : dq) {
			if (s.value().equals(value)) {
				return s.testId();
			}
		}
		return null;
	}

	/**
	 * Record {@code value} as seen now in test {@code testId} for ({@code namespace}, {@code scope})
	 * (prunes + enforces the FIFO cap).
	 */
	static synchronized void record(String namespace, String scope, String value, String testId) {
		Deque<Seen> dq = ENTRIES.computeIfAbsent(key(namespace, scope), k -> new ArrayDeque<>());
		prune(dq);
		dq.addLast(new Seen(value, testId == null ? "" : testId, Instant.now()));
		while (dq.size() > MAX_PER_SCOPE) {
			dq.removeFirst();
		}
	}

	private static void prune(Deque<Seen> dq) {
		Instant cutoff = Instant.now().minus(retention.get());
		while (!dq.isEmpty() && dq.peekFirst().at().isBefore(cutoff)) {
			dq.removeFirst();
		}
	}

	/** Drop all history AND reset the testing retention override. Call from {@code @BeforeEach}. */
	public static synchronized void clear() {
		ENTRIES.clear();
		retention.set(DEFAULT_RETENTION);
	}

	/**
	 * Override the retention window for the lifetime of the JVM (or until {@link #clear}).
	 * Intended for unit tests so they don't have to wait hours. Production paths must not touch this.
	 */
	public static void setRetentionForTesting(Duration newRetention) {
		retention.set(newRetention);
	}
}
