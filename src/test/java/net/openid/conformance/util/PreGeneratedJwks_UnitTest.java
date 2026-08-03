package net.openid.conformance.util;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PreGeneratedJwks_UnitTest {

	@BeforeEach
	void resetState() {
		PreGeneratedJwks.clear();
	}

	@AfterEach
	void tearDown() {
		PreGeneratedJwks.clear();
	}

	private static Environment env(String sub, String iss) {
		Environment e = new Environment();
		e.putString("owner_sub", sub);
		e.putString("owner_iss", iss);
		return e;
	}

	@Test
	void missingOwnerSubOrIssThrows() {
		Environment e1 = new Environment();
		assertThrows(IllegalStateException.class, () -> PreGeneratedJwks.nextRsaKey(e1, 2048));

		Environment e2 = new Environment();
		e2.putString("owner_sub", "only-sub");
		assertThrows(IllegalStateException.class, () -> PreGeneratedJwks.nextRsaKey(e2, 2048));
	}

	@Test
	void sequentialCallsOnSameEnvReturnDifferentKeys() throws Exception {
		Environment env = env("alice", "issuer-a");
		RSAKey k0 = PreGeneratedJwks.nextRsaKey(env, 2048);
		RSAKey k1 = PreGeneratedJwks.nextRsaKey(env, 2048);
		assertNotEquals(k0.computeThumbprint(), k1.computeThumbprint());
	}

	@Test
	void freshEnvsSameOwnerHitTheCache() throws Exception {
		Environment a = env("alice", "issuer-a");
		Environment b = env("alice", "issuer-a");
		RSAKey ka = PreGeneratedJwks.nextRsaKey(a, 2048);
		RSAKey kb = PreGeneratedJwks.nextRsaKey(b, 2048);
		assertEquals(ka.computeThumbprint(), kb.computeThumbprint(),
			"Two envs with the same owner both starting at counter 0 should share the cached key");
	}

	@Test
	void differentOwnersGetDifferentKeys() throws Exception {
		Environment alice = env("alice", "issuer-a");
		Environment bob = env("bob", "issuer-b");
		RSAKey ka = PreGeneratedJwks.nextRsaKey(alice, 2048);
		RSAKey kb = PreGeneratedJwks.nextRsaKey(bob, 2048);
		assertNotEquals(ka.computeThumbprint(), kb.computeThumbprint());
	}

	@Test
	void perSlotCountersAreIndependent() throws Exception {
		Environment env = env("alice", "issuer-a");
		// Bump RSA counter twice
		PreGeneratedJwks.nextRsaKey(env, 2048);
		PreGeneratedJwks.nextRsaKey(env, 2048);
		// EC counter should still be at 0 — same key as a fresh env's first EC key.
		ECKey ec0 = PreGeneratedJwks.nextEcKey(env, Curve.P_256);
		Environment fresh = env("alice", "issuer-a");
		ECKey freshEc0 = PreGeneratedJwks.nextEcKey(fresh, Curve.P_256);
		assertEquals(ec0.computeThumbprint(), freshEc0.computeThumbprint());
	}

	@Test
	void capExceededThrows() {
		Environment env = env("alice", "issuer-a");
		// Burn through the cap
		for (int i = 0; i < PreGeneratedJwks.MAX_INDEX_PER_NAMESPACE; i++) {
			PreGeneratedJwks.nextRsaKey(env, 2048);
		}
		IllegalStateException ex = assertThrows(IllegalStateException.class,
			() -> PreGeneratedJwks.nextRsaKey(env, 2048));
		assertTrue(ex.getMessage().contains("rsa-2048"), "message should name the slot: " + ex.getMessage());
		assertTrue(ex.getMessage().contains(Integer.toString(PreGeneratedJwks.MAX_INDEX_PER_NAMESPACE)),
			"message should name the cap: " + ex.getMessage());
	}

	@Test
	void ttlExpiryEvictsAndRegenerates() throws Exception {
		AtomicLong now = new AtomicLong(1_000_000_000L);  // arbitrary base
		PreGeneratedJwks.setTickerForTesting(now::get);
		PreGeneratedJwks.setRetentionForTesting(Duration.ofMillis(50));

		Environment e1 = env("alice", "issuer-a");
		RSAKey k1 = PreGeneratedJwks.nextRsaKey(e1, 2048);

		// Advance well beyond retention.
		now.addAndGet(Duration.ofMillis(200).toNanos());

		Environment e2 = env("alice", "issuer-a");
		RSAKey k2 = PreGeneratedJwks.nextRsaKey(e2, 2048);

		assertNotEquals(k1.computeThumbprint(), k2.computeThumbprint(),
			"After retention elapses the entry should be regenerated");
	}

	@Test
	void opportunisticSweepDropsIdleNamespaces() throws Exception {
		AtomicLong now = new AtomicLong(1_000_000_000L);
		PreGeneratedJwks.setTickerForTesting(now::get);
		PreGeneratedJwks.setRetentionForTesting(Duration.ofMillis(50));
		PreGeneratedJwks.setSweepIntervalForTesting(Duration.ofMillis(100));

		RSAKey bobOriginal = PreGeneratedJwks.nextRsaKey(env("bob", "issuer-b"), 2048);

		// Advance past retention + sweep interval, then touch a different owner so the next
		// borrow runs maybeSweep(), which should expire and drop Bob's idle namespace entirely.
		now.addAndGet(Duration.ofMillis(200).toNanos());
		PreGeneratedJwks.nextRsaKey(env("alice", "issuer-a"), 2048);

		// Bob's entry was swept, so a fresh draw at index 0 regenerates a different key.
		RSAKey bobAgain = PreGeneratedJwks.nextRsaKey(env("bob", "issuer-b"), 2048);
		assertNotEquals(bobOriginal.computeThumbprint(), bobAgain.computeThumbprint(),
			"Idle namespace should be swept and the key regenerated on next use");
	}

	@Test
	void singleFlightRunsGeneratorOnceForConcurrentMisses() throws Exception {
		// Inject a generator that blocks while the winner holds the in-flight slot, then race
		// a second caller against it. With single-flight the loser must wait on the winner's
		// future rather than run its own generation, so exactly one generation occurs.
		AtomicInteger generations = new AtomicInteger();
		CountDownLatch winnerInGenerator = new CountDownLatch(1);
		CountDownLatch releaseGenerator = new CountDownLatch(1);
		Supplier<JWK> slowGenerator = () -> {
			generations.incrementAndGet();
			winnerInGenerator.countDown();
			try {
				releaseGenerator.await();
				return new RSAKeyGenerator(2048).generate();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException(e);
			} catch (JOSEException e) {
				throw new IllegalStateException(e);
			}
		};

		ExecutorService pool = Executors.newFixedThreadPool(2);
		AtomicReference<Thread> loserThread = new AtomicReference<>();
		try {
			Future<JWK> winner = pool.submit(() ->
				PreGeneratedJwks.borrowForTesting("rsa-2048", 0, slowGenerator));

			// Winner is now inside the generator, holding the in-flight slot.
			assertTrue(winnerInGenerator.await(30, TimeUnit.SECONDS), "winner never entered the generator");

			Future<JWK> loser = pool.submit(() -> {
				loserThread.set(Thread.currentThread());
				return PreGeneratedJwks.borrowForTesting("rsa-2048", 0, slowGenerator);
			});

			// Wait until the loser is parked on the winner's in-flight future (its only blocking
			// point), so releasing the winner cannot let the loser win a fresh slot and generate.
			long deadline = System.currentTimeMillis() + 30_000;
			Thread t;
			while ((t = loserThread.get()) == null
					|| (t.getState() != Thread.State.WAITING && t.getState() != Thread.State.TIMED_WAITING)) {
				assertTrue(System.currentTimeMillis() < deadline, "loser never blocked on the in-flight future");
				Thread.sleep(5);
			}

			releaseGenerator.countDown();

			JWK winnerKey = winner.get(30, TimeUnit.SECONDS);
			JWK loserKey = loser.get(30, TimeUnit.SECONDS);

			assertEquals(1, generations.get(),
				"only one generation should run for concurrent misses on the same (slot, index)");
			assertEquals(winnerKey.computeThumbprint(), loserKey.computeThumbprint(),
				"the loser must read back the winner's cached JWK");
		} finally {
			releaseGenerator.countDown();  // unblock any thread still waiting if an assertion failed
			pool.shutdownNow();
		}
	}

	@Test
	void clearResetsEverything() throws Exception {
		AtomicInteger generations = new AtomicInteger();
		Supplier<JWK> countingGenerator = () -> {
			generations.incrementAndGet();
			try {
				return new RSAKeyGenerator(2048).generate();
			} catch (JOSEException e) {
				throw new IllegalStateException(e);
			}
		};

		PreGeneratedJwks.borrowForTesting("rsa-2048", 0, countingGenerator);
		PreGeneratedJwks.borrowForTesting("rsa-2048", 0, countingGenerator);
		assertEquals(1, generations.get(), "second borrow at the same (slot, index) should hit the cache");

		PreGeneratedJwks.clear();

		// After clear() the cached entry is gone, so the next borrow regenerates.
		PreGeneratedJwks.borrowForTesting("rsa-2048", 0, countingGenerator);
		assertEquals(2, generations.get(), "clear() should drop cached entries so the next borrow regenerates");
		// clear() also restores the tunables to their defaults.
		assertEquals(Duration.ofHours(3), PreGeneratedJwks.retention());
	}

	@Test
	void clientPrivateKeysDistinctnessGate() throws Exception {
		// ValidateClientPrivateKeysAreDifferent is the framework's hard gate; this test
		// just confirms two back-to-back nextRsaKey calls on one env give different
		// thumbprints — the exact property the gate checks.
		Environment env = env("alice", "issuer-a");
		RSAKey clientKey = PreGeneratedJwks.nextRsaKey(env, 2048);
		RSAKey client2Key = PreGeneratedJwks.nextRsaKey(env, 2048);
		assertNotEquals(clientKey.computeThumbprint(), client2Key.computeThumbprint());
	}

	@Test
	void loserTimesOutWhenWinnersGeneratorHangs() throws Exception {
		PreGeneratedJwks.setGenerationTimeoutForTesting(Duration.ofMillis(200));
		CountDownLatch winnerInGenerator = new CountDownLatch(1);
		CountDownLatch releaseWinner = new CountDownLatch(1);
		Supplier<JWK> hangingGenerator = () -> {
			winnerInGenerator.countDown();
			try {
				releaseWinner.await();
				return new RSAKeyGenerator(2048).generate();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException(e);
			} catch (JOSEException e) {
				throw new IllegalStateException(e);
			}
		};

		ExecutorService pool = Executors.newFixedThreadPool(2);
		try {
			pool.submit(() -> PreGeneratedJwks.borrowForTesting("rsa-2048", 0, hangingGenerator));
			assertTrue(winnerInGenerator.await(30, TimeUnit.SECONDS), "winner never entered the generator");

			// The winner holds the in-flight slot and is stuck; the loser must time out and
			// throw rather than block forever.
			Future<JWK> loser = pool.submit(() ->
				PreGeneratedJwks.borrowForTesting("rsa-2048", 0, hangingGenerator));
			ExecutionException ex = assertThrows(ExecutionException.class, () -> loser.get(30, TimeUnit.SECONDS));
			Throwable cause = ex.getCause();
			assertTrue(cause instanceof IllegalStateException, "cause should be IllegalStateException: " + cause);
			assertTrue(cause.getMessage().contains("Timed out"),
				"expected a timeout error, got: " + cause.getMessage());
		} finally {
			releaseWinner.countDown();  // unblock the winner so its thread doesn't linger
			pool.shutdownNow();
		}
	}
}
