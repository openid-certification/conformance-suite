package net.openid.conformance.condition.client;

import org.springframework.http.HttpHeaders;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

/**
 * Process-wide in-memory cache for outgoing HTTP responses (used by the
 * {@link CachingHttpInterceptor} to avoid re-fetching stable OP metadata
 * like {@code /.well-known/openid-configuration}, JWKS sets, and VCI
 * issuer metadata).
 *
 * <p>The eligibility decision (which requests to cache) lives in
 * {@link CachingHttpInterceptor}; this class is a dumb store keyed by an
 * opaque string the interceptor builds.
 *
 * <p><b>Single-flight on miss:</b> concurrent {@link #getOrFetch} calls
 * for the same key share one fetch via a per-key in-flight
 * {@link CompletableFuture}, so CI runs with many parallel test plans
 * against the same upstream OP issue at most one HTTP call per URL.
 *
 * <p><b>Successful responses only.</b> If the supplied fetcher throws,
 * nothing is cached and the next call invokes the fetcher again (no
 * negative caching). The exception propagates to ALL waiting callers.
 *
 * <p>Entries expire after {@link #DEFAULT_TTL} (1 hour) unless overridden
 * via {@link #setTtlForTesting}. {@link #clear} resets the TTL override
 * back to the default so tests cannot leak overrides into one another.
 *
 * <p>Singleton because conformance-suite conditions are instantiated via
 * reflective {@code newInstance()} with no Spring DI; a static facade is
 * the simplest way to share state across instances.
 */
public final class ExternalEndpointCache {

	private static final Duration DEFAULT_TTL = Duration.ofHours(1);

	private static final Map<String, Entry> ENTRIES = new ConcurrentHashMap<>();
	private static final Map<String, CompletableFuture<Entry>> IN_FLIGHT = new ConcurrentHashMap<>();
	private static final AtomicReference<Duration> ttl = new AtomicReference<>(DEFAULT_TTL);

	private ExternalEndpointCache() {}

	/** One cached HTTP response. {@code body} is the raw response bytes
	 *  (treat as immutable - never mutate the array after construction);
	 *  {@code headers} is a defensive copy (the originating
	 *  {@code HttpHeaders} from Apache HttpClient is not thread-safe). */
	@SuppressWarnings("ArrayRecordComponent") // body is treated as immutable; no equality on Entry
	public record Entry(byte[] body, int statusCode, String statusText, HttpHeaders headers, Instant cachedAt) {
		/** Wall-clock seconds since this entry was cached. */
		public long ageSeconds() {
			return Duration.between(cachedAt, Instant.now()).toSeconds();
		}
	}

	/** A fetcher that produces an {@link Entry} or throws on any failure
	 *  (network error, etc.). */
	@FunctionalInterface
	public interface Fetcher {
		Entry fetch() throws Exception;
	}

	/** Returns the cached entry for {@code key} if present and not yet
	 *  expired; otherwise empty. */
	public static Optional<Entry> get(String key) {
		Entry e = ENTRIES.get(key);
		if (e == null) {
			return Optional.empty();
		}
		if (Duration.between(e.cachedAt, Instant.now()).compareTo(ttl.get()) > 0) {
			ENTRIES.remove(key, e);
			return Optional.empty();
		}
		return Optional.of(e);
	}

	/** Return the cached entry for {@code key} if fresh; otherwise invoke
	 *  {@code fetcher} and cache the result. Concurrent misses for the
	 *  same key share one fetch. Convenience wrapper that always caches
	 *  the fetched entry; use {@link #getOrFetch(String, Fetcher, Predicate)}
	 *  to gate caching on a per-entry predicate. */
	public static Entry getOrFetch(String key, Fetcher fetcher) throws Exception {
		return getOrFetch(key, fetcher, e -> true);
	}

	/** Like {@link #getOrFetch(String, Fetcher)} but only commits the
	 *  fetched entry to the cache when {@code shouldCache.test(entry)}
	 *  returns true. Single-flight protection still applies — concurrent
	 *  callers share one fetch regardless of the predicate's verdict —
	 *  but a "don't cache" verdict means the next call after the elected
	 *  fetcher completes will re-fetch. Typical use: don't cache non-2xx
	 *  HTTP responses. */
	public static Entry getOrFetch(String key, Fetcher fetcher, Predicate<Entry> shouldCache) throws Exception {
		Optional<Entry> hit = get(key);
		if (hit.isPresent()) {
			return hit.get();
		}

		CompletableFuture<Entry> mine = new CompletableFuture<>();
		CompletableFuture<Entry> existing = IN_FLIGHT.putIfAbsent(key, mine);

		if (existing != null) {
			try {
				return existing.get();
			} catch (ExecutionException e) {
				Throwable cause = e.getCause() != null ? e.getCause() : e;
				if (cause instanceof Exception ex) {
					throw ex;
				}
				throw new RuntimeException(cause);
			}
		}

		// We're the elected fetcher.
		try {
			Entry fresh = fetcher.fetch();
			if (fresh == null) {
				throw new IllegalStateException("fetcher returned null Entry for " + key);
			}
			if (shouldCache.test(fresh)) {
				ENTRIES.put(key, fresh);
			}
			mine.complete(fresh);
			return fresh;
		} catch (Throwable t) {
			mine.completeExceptionally(t);
			throw t;
		} finally {
			IN_FLIGHT.remove(key, mine);
		}
	}

	/** Drop every cached entry AND reset the testing TTL override.
	 *  Intended for test isolation - call from {@code @BeforeEach}. */
	public static void clear() {
		ENTRIES.clear();
		IN_FLIGHT.clear();
		ttl.set(DEFAULT_TTL);
	}

	/** Override the TTL for the lifetime of the JVM (or until {@link #clear}
	 *  is called). Intended for unit tests so they don't have to wait an
	 *  hour. Production paths should not touch this. */
	public static void setTtlForTesting(Duration newTtl) {
		ttl.set(newTtl);
	}
}
