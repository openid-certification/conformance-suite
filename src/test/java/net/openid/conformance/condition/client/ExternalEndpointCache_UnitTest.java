package net.openid.conformance.condition.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExternalEndpointCache_UnitTest {

	@BeforeEach
	void clearCache() {
		// clear() must reset the TTL override too; otherwise a prior test
		// that called setTtlForTesting could pollute later tests.
		ExternalEndpointCache.clear();
	}

	private static ExternalEndpointCache.Entry entry(String body) {
		return new ExternalEndpointCache.Entry(body.getBytes(), 200, "OK",
			HttpHeaders.readOnlyHttpHeaders(new HttpHeaders()), Instant.now());
	}

	@Test
	void missReturnsEmpty() {
		Optional<ExternalEndpointCache.Entry> e = ExternalEndpointCache.get("https://example/missing");
		assertTrue(e.isEmpty());
	}

	@Test
	void getOrFetchPopulatesAndReturnsEntry() throws Exception {
		ExternalEndpointCache.Entry result = ExternalEndpointCache.getOrFetch("https://example/x",
			() -> entry("{\"a\":1}"));
		assertArrayEquals("{\"a\":1}".getBytes(), result.body());

		Optional<ExternalEndpointCache.Entry> hit = ExternalEndpointCache.get("https://example/x");
		assertTrue(hit.isPresent());
		assertArrayEquals("{\"a\":1}".getBytes(), hit.get().body());
	}

	@Test
	void getOrFetchStoresStatusAndHeaders() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Type", "application/json");
		ExternalEndpointCache.getOrFetch("https://example/x",
			() -> new ExternalEndpointCache.Entry("body".getBytes(), 200, "OK",
				HttpHeaders.readOnlyHttpHeaders(headers), Instant.now()));

		ExternalEndpointCache.Entry hit = ExternalEndpointCache.get("https://example/x").get();
		assertEquals(200, hit.statusCode());
		assertEquals("OK", hit.statusText());
		assertEquals("application/json", hit.headers().getFirst("Content-Type"));
	}

	@Test
	void getOrFetchDoesNotInvokeFetcherOnHit() throws Exception {
		AtomicInteger fetcherCalls = new AtomicInteger();
		ExternalEndpointCache.getOrFetch("https://example/x", () -> {
			fetcherCalls.incrementAndGet();
			return entry("body");
		});
		ExternalEndpointCache.getOrFetch("https://example/x", () -> {
			fetcherCalls.incrementAndGet();
			return entry("body");
		});
		assertEquals(1, fetcherCalls.get(), "second call must be a cache hit");
	}

	@Test
	void getOrFetchSingleFlight_concurrentMissesInvokeFetcherOnce() throws Exception {
		AtomicInteger fetcherCalls = new AtomicInteger();
		CountDownLatch fetcherEntered = new CountDownLatch(1);
		CountDownLatch fetcherMayReturn = new CountDownLatch(1);

		ExecutorService pool = Executors.newFixedThreadPool(2);
		try {
			Future<ExternalEndpointCache.Entry> f1 = pool.submit(() ->
				ExternalEndpointCache.getOrFetch("https://example/flight", () -> {
					fetcherCalls.incrementAndGet();
					fetcherEntered.countDown();
					fetcherMayReturn.await();
					return entry("first");
				}));
			assertTrue(fetcherEntered.await(2, TimeUnit.SECONDS));

			Future<ExternalEndpointCache.Entry> f2 = pool.submit(() ->
				ExternalEndpointCache.getOrFetch("https://example/flight", () -> {
					fetcherCalls.incrementAndGet();
					return entry("second");
				}));

			Thread.sleep(50);
			fetcherMayReturn.countDown();

			ExternalEndpointCache.Entry r1 = f1.get(2, TimeUnit.SECONDS);
			ExternalEndpointCache.Entry r2 = f2.get(2, TimeUnit.SECONDS);

			assertEquals(1, fetcherCalls.get(), "second caller must reuse the in-flight fetch");
			assertArrayEquals("first".getBytes(), r1.body());
			assertArrayEquals("first".getBytes(), r2.body(), "second caller must receive the same entry");
		} finally {
			pool.shutdownNow();
		}
	}

	@Test
	void getOrFetchWithPredicateSkipsCacheWhenPredicateRejects() throws Exception {
		ExternalEndpointCache.Entry returned = ExternalEndpointCache.getOrFetch("https://example/transient",
			() -> new ExternalEndpointCache.Entry("err".getBytes(), 503, "Service Unavailable",
				HttpHeaders.readOnlyHttpHeaders(new HttpHeaders()), Instant.now()),
			e -> e.statusCode() >= 200 && e.statusCode() < 300);
		// caller still gets the entry
		assertEquals(503, returned.statusCode());
		// but the cache stays empty so the next call re-fetches
		assertTrue(ExternalEndpointCache.get("https://example/transient").isEmpty(),
			"503 must not be cached when predicate rejects");
	}

	@Test
	void getOrFetchWithPredicateCachesWhenPredicateAccepts() throws Exception {
		ExternalEndpointCache.getOrFetch("https://example/ok",
			() -> new ExternalEndpointCache.Entry("body".getBytes(), 200, "OK",
				HttpHeaders.readOnlyHttpHeaders(new HttpHeaders()), Instant.now()),
			e -> e.statusCode() >= 200 && e.statusCode() < 300);
		assertTrue(ExternalEndpointCache.get("https://example/ok").isPresent(),
			"200 must be cached when predicate accepts");
	}

	@Test
	void getOrFetchFailureIsNotCachedAndIsPropagated() {
		RuntimeException boom = assertThrows(RuntimeException.class,
			() -> ExternalEndpointCache.getOrFetch("https://example/bad", () -> {
				throw new RuntimeException("network");
			}));
		assertEquals("network", boom.getMessage());

		assertTrue(ExternalEndpointCache.get("https://example/bad").isEmpty(),
			"failed fetch must NOT be cached");

		AtomicInteger calls = new AtomicInteger();
		assertThrows(RuntimeException.class,
			() -> ExternalEndpointCache.getOrFetch("https://example/bad", () -> {
				calls.incrementAndGet();
				throw new RuntimeException("still broken");
			}));
		assertEquals(1, calls.get());
	}

	@Test
	void getReturnsCachedAtTimestamp() throws Exception {
		Instant before = Instant.now();
		ExternalEndpointCache.getOrFetch("https://example/x", () -> entry("body"));
		Optional<ExternalEndpointCache.Entry> e = ExternalEndpointCache.get("https://example/x");
		assertTrue(e.isPresent());
		assertFalse(e.get().cachedAt().isBefore(before));
	}

	@Test
	void getReturnsEmptyAfterTtlElapsed() throws Exception {
		ExternalEndpointCache.setTtlForTesting(Duration.ofMillis(50));
		ExternalEndpointCache.getOrFetch("https://example/short-lived", () -> entry("body"));
		Thread.sleep(120);
		assertTrue(ExternalEndpointCache.get("https://example/short-lived").isEmpty());
	}

	@Test
	void differentKeysAreCachedIndependently() throws Exception {
		ExternalEndpointCache.getOrFetch("https://example/a", () -> entry("A"));
		ExternalEndpointCache.getOrFetch("https://example/b", () -> entry("B"));
		assertArrayEquals("A".getBytes(), ExternalEndpointCache.get("https://example/a").get().body());
		assertArrayEquals("B".getBytes(), ExternalEndpointCache.get("https://example/b").get().body());
	}

	@Test
	void clearEvictsAllEntries() throws Exception {
		ExternalEndpointCache.getOrFetch("https://example/a", () -> entry("A"));
		ExternalEndpointCache.getOrFetch("https://example/b", () -> entry("B"));
		ExternalEndpointCache.clear();
		assertTrue(ExternalEndpointCache.get("https://example/a").isEmpty());
		assertTrue(ExternalEndpointCache.get("https://example/b").isEmpty());
	}

	@Test
	void clearAlsoResetsTtlOverride() throws Exception {
		ExternalEndpointCache.setTtlForTesting(Duration.ofMillis(1));
		ExternalEndpointCache.clear();
		ExternalEndpointCache.getOrFetch("https://example/long-lived", () -> entry("body"));
		Thread.sleep(50);
		assertTrue(ExternalEndpointCache.get("https://example/long-lived").isPresent(),
			"clear() must reset the TTL override; otherwise it leaks across tests");
	}
}
