package net.openid.conformance.util;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.bc.BouncyCastleProviderSingleton;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.jwk.gen.OctetKeyPairGenerator;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import net.openid.conformance.testmodule.Environment;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Owner-scoped, namespace-keyed cache of pre-generated JWK keypairs.
 *
 * <p>Conditions that previously generated a fresh keypair per call (RSA/EC/OKP
 * for client JWKs, server JWKs, DPoP, encryption keys, etc.) instead call
 * {@link #nextRsaKey} / {@link #nextEcKey} / {@link #nextOkpKey} here, which
 * returns a cached key for the current authenticated user. The cache key is
 * {@code (owner, slot, index)} where {@code owner} is the suite user's
 * {@code (sub, iss)} pair, {@code slot} is a constant string per
 * (kty, curve, key size), and {@code index} is a monotonic per-test-instance
 * counter on {@link Environment}, scoped per slot (each slot's indices start
 * at 0 independently).
 *
 * <p><b>Distinctness contract.</b> Within a single test instance, successive
 * calls for the same slot return entries at different indices, so two
 * back-to-back calls always return different keys. This satisfies the
 * framework's hard correctness gate {@code ValidateClientPrivateKeysAreDifferent}
 * (compares {@code client} vs {@code client2} thumbprints within one test).
 *
 * <p><b>Cap and overflow.</b> Hard cap of {@value #MAX_INDEX_PER_NAMESPACE}
 * distinct keys per {@code (owner, slot)} within a single test instance. A
 * request with index ≥ cap throws {@link IllegalStateException}. No silent
 * modulo wrap — silent recycling would hide kid collisions in callers like
 * {@code VCIPrepareBatchProofKeys} and {@code keyIDFromThumbprint(true)}
 * paths.
 *
 * <p><b>Owner required.</b> If {@code env} is missing
 * {@code owner_sub} / {@code owner_iss} (set by
 * {@code AbstractTestModule.exposeOwnerIdToEnvironment()}), the public API
 * throws. Anonymous mode is an explicit test-only API
 * ({@link #borrowForTesting}) that callers must opt into.
 *
 * <p><b>TTL.</b> Entries are evicted when not touched within
 * {@link #retention()}. An opportunistic sweep runs once per
 * {@code sweepInterval} on any read — no dedicated thread.
 *
 * <p><b>Concurrency.</b> Single-flight generation via per-{@link CacheKey}
 * {@link CompletableFuture}. Two threads racing for an absent {@code (owner,
 * slot, idx)} only run one generator; the loser awaits and reads back the
 * same JWK.
 *
 * <p><b>Same-user parallel-test sharing.</b> Two parallel tests for the same
 * authenticated user both start at slot index 0 and therefore see the same
 * cached key. This is correctness-safe (no test inspects raw private bytes
 * across tests; thumbprint comparisons happen within a test) and is the
 * design we want — it minimises keygen across the JVM.
 */
public final class PreGeneratedJwks {

	/** Per-{@code (owner, slot)} distinct-key cap. Exceeding it throws. */
	public static final int MAX_INDEX_PER_NAMESPACE = 32;

	private static final Duration DEFAULT_RETENTION = Duration.ofHours(3);
	private static final Duration DEFAULT_SWEEP_INTERVAL = Duration.ofMinutes(15);

	/** How long a losing thread waits for the winning thread's in-flight generation
	 *  before failing rather than blocking forever. Far above any real keygen
	 *  (RSA-2048 is tens of ms); exists only so a stuck generator surfaces as one
	 *  clear error instead of letting blocked waiters accumulate with no cause. */
	private static final Duration DEFAULT_GENERATION_TIMEOUT = Duration.ofMinutes(3);

	private static volatile Duration retention = DEFAULT_RETENTION;
	private static volatile Duration sweepInterval = DEFAULT_SWEEP_INTERVAL;
	private static volatile Duration generationTimeout = DEFAULT_GENERATION_TIMEOUT;
	private static volatile LongSupplier ticker = System::nanoTime;

	private record OwnerKey(String sub, String iss) {
		OwnerKey {
			Objects.requireNonNull(sub, "sub");
			Objects.requireNonNull(iss, "iss");
		}
	}

	private record NamespaceKey(OwnerKey owner, String slot) {
		NamespaceKey {
			Objects.requireNonNull(owner, "owner");
			Objects.requireNonNull(slot, "slot");
		}
	}

	private record CacheKey(NamespaceKey namespace, int index) {
	}

	private record Entry(JWK key, AtomicLong lastUsedAtNanos) {
	}

	private static final ConcurrentHashMap<NamespaceKey, ConcurrentHashMap<Integer, Entry>> CACHE = new ConcurrentHashMap<>();
	private static final ConcurrentHashMap<CacheKey, CompletableFuture<JWK>> IN_FLIGHT = new ConcurrentHashMap<>();
	private static final AtomicLong LAST_SWEEP_NANOS = new AtomicLong(0);

	/** Sentinel owner used by the {@code *ForTesting} API. Never resolvable
	 *  from a real {@link Environment}. */
	private static final OwnerKey ANONYMOUS_TEST_OWNER = new OwnerKey("_test", "_test");

	private PreGeneratedJwks() {
	}

	// ===== Public API =====

	public static RSAKey nextRsaKey(Environment env, int keySize) {
		OwnerKey owner = resolveOwnerOrThrow(env);
		String slot = rsaSlot(keySize);
		int idx = nextIndex(env, slot);
		return (RSAKey) borrow(new NamespaceKey(owner, slot), idx,
			() -> generateRsa(keySize));
	}

	public static ECKey nextEcKey(Environment env, Curve curve) {
		Objects.requireNonNull(curve, "curve");
		OwnerKey owner = resolveOwnerOrThrow(env);
		String slot = ecSlot(curve);
		int idx = nextIndex(env, slot);
		return (ECKey) borrow(new NamespaceKey(owner, slot), idx,
			() -> generateEc(curve));
	}

	public static OctetKeyPair nextOkpKey(Environment env, Curve curve) {
		Objects.requireNonNull(curve, "curve");
		OwnerKey owner = resolveOwnerOrThrow(env);
		String slot = okpSlot(curve);
		int idx = nextIndex(env, slot);
		return (OctetKeyPair) borrow(new NamespaceKey(owner, slot), idx,
			() -> generateOkp(curve));
	}

	// ===== Test-only API =====

	/** Test-only: drive the single-flight {@link #borrow} path with an explicit
	 *  generator and {@code (slot, index)}, bypassing owner resolution and the
	 *  per-test counter. Lets a test prove exactly one generation runs for
	 *  concurrent misses on the same key. */
	static JWK borrowForTesting(String slot, int index, Supplier<JWK> generator) {
		return borrow(new NamespaceKey(ANONYMOUS_TEST_OWNER, slot), index, generator);
	}

	static void setRetentionForTesting(Duration r) {
		retention = Objects.requireNonNull(r);
	}

	static void setSweepIntervalForTesting(Duration s) {
		sweepInterval = Objects.requireNonNull(s);
	}

	static void setTickerForTesting(LongSupplier t) {
		ticker = Objects.requireNonNull(t);
	}

	static void setGenerationTimeoutForTesting(Duration t) {
		generationTimeout = Objects.requireNonNull(t);
	}

	/** Test-only: reset everything to defaults. */
	static void clear() {
		CACHE.clear();
		IN_FLIGHT.clear();
		LAST_SWEEP_NANOS.set(0);
		retention = DEFAULT_RETENTION;
		sweepInterval = DEFAULT_SWEEP_INTERVAL;
		generationTimeout = DEFAULT_GENERATION_TIMEOUT;
		ticker = System::nanoTime;
	}

	static Duration retention() { return retention; }

	// ===== Internals =====

	private static OwnerKey resolveOwnerOrThrow(Environment env) {
		Objects.requireNonNull(env, "env");
		String sub = env.getString("owner_sub");
		String iss = env.getString("owner_iss");
		if (sub == null || iss == null) {
			throw new IllegalStateException(
				"PreGeneratedJwks: owner_sub/owner_iss are missing from the environment. "
					+ "AbstractTestModule.exposeOwnerIdToEnvironment() should have set them. "
					+ "If this is a unit-test context, use the *ForTesting helpers instead.");
		}
		return new OwnerKey(sub, iss);
	}

	private static int nextIndex(Environment env, String slot) {
		int idx = env.nextSystemCounter("PreGeneratedJwks." + slot);
		if (idx >= MAX_INDEX_PER_NAMESPACE) {
			throw new IllegalStateException(
				"PreGeneratedJwks: per-test-instance cap of " + MAX_INDEX_PER_NAMESPACE
					+ " distinct " + slot + " keys exceeded (requested index " + idx + "). "
					+ "Either raise PreGeneratedJwks.MAX_INDEX_PER_NAMESPACE or refactor the test "
					+ "to need fewer distinct keys per instance.");
		}
		return idx;
	}

	private static JWK borrow(NamespaceKey namespace, int index, Supplier<JWK> generator) {
		maybeSweep();

		// Fast path: hot cache hit, refresh lastUsed, return.
		Entry hit = lookupFresh(namespace, index);
		if (hit != null) {
			hit.lastUsedAtNanos().set(nowNanos());
			return hit.key();
		}

		// Cold path: single-flight on (namespace, index).
		CacheKey cacheKey = new CacheKey(namespace, index);
		CompletableFuture<JWK> mine = new CompletableFuture<>();
		CompletableFuture<JWK> winner = IN_FLIGHT.putIfAbsent(cacheKey, mine);
		if (winner != null) {
			// Lost the race — wait for the other thread's generation, but bounded: if the
			// winner's generator is stuck, fail with a clear error rather than blocking
			// forever and letting losing threads accumulate with no obvious cause.
			try {
				return winner.get(generationTimeout.toMillis(), TimeUnit.MILLISECONDS);
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException("Interrupted awaiting cached JWK generation", ie);
			} catch (ExecutionException ee) {
				Throwable cause = ee.getCause();
				if (cause instanceof RuntimeException re) {
					throw re;
				}
				throw new IllegalStateException("PreGeneratedJwks generation failed", cause);
			} catch (TimeoutException te) {
				throw new IllegalStateException("Timed out after " + generationTimeout
					+ " waiting for another thread to generate a " + namespace.slot()
					+ " keypair; the generating thread appears stuck", te);
			}
		}

		// Won the in-flight slot. Re-check the cache before generating: another thread may
		// have generated, stored the entry, and removed its future between our cache miss
		// above and our putIfAbsent win here. The winner removes its future only after the
		// store, so once this slot is free again the entry is already visible — without this
		// recheck that interleaving would generate a second key for the same
		// (owner, slot, index), breaking the "concurrent missers share one key" contract.
		try {
			Entry raced = lookupFresh(namespace, index);
			if (raced != null) {
				raced.lastUsedAtNanos().set(nowNanos());
				mine.complete(raced.key());
				return raced.key();
			}
			JWK fresh = generator.get();
			Entry entry = new Entry(fresh, new AtomicLong(nowNanos()));
			// Atomic get-or-create-and-put under the namespace bin lock, so it cannot interleave
			// with maybeSweep()'s computeIfPresent removal of an empty namespace (which holds the
			// same bin lock). A separate computeIfAbsent(...).put(...) would let a concurrent sweep
			// detach the inner map between the two steps, stranding this entry outside the cache.
			CACHE.compute(namespace, (k, existing) -> {
				ConcurrentHashMap<Integer, Entry> ns = (existing != null) ? existing : new ConcurrentHashMap<>();
				ns.put(index, entry);
				return ns;
			});
			mine.complete(fresh);
			return fresh;
		} catch (RuntimeException re) {
			mine.completeExceptionally(re);
			throw re;
		} catch (Throwable t) {
			mine.completeExceptionally(t);
			throw new IllegalStateException("PreGeneratedJwks generation failed", t);
		} finally {
			IN_FLIGHT.remove(cacheKey, mine);
		}
	}

	/** Returns the cached entry for {@code (namespace, index)} if present and not
	 *  expired, otherwise null. */
	private static Entry lookupFresh(NamespaceKey namespace, int index) {
		ConcurrentHashMap<Integer, Entry> ns = CACHE.get(namespace);
		if (ns != null) {
			Entry e = ns.get(index);
			if (e != null && !isExpired(e)) {
				return e;
			}
		}
		return null;
	}

	private static boolean isExpired(Entry e) {
		return (nowNanos() - e.lastUsedAtNanos().get()) > retention.toNanos();
	}

	private static void maybeSweep() {
		long now = nowNanos();
		long prev = LAST_SWEEP_NANOS.get();
		if (prev != 0 && (now - prev) < sweepInterval.toNanos()) {
			return;
		}
		if (!LAST_SWEEP_NANOS.compareAndSet(prev, now)) {
			// Another thread won the right to sweep this interval.
			return;
		}
		List<NamespaceKey> emptyNamespaces = new ArrayList<>();
		for (Map.Entry<NamespaceKey, ConcurrentHashMap<Integer, Entry>> nsEntry : CACHE.entrySet()) {
			ConcurrentHashMap<Integer, Entry> ns = nsEntry.getValue();
			Iterator<Map.Entry<Integer, Entry>> it = ns.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<Integer, Entry> indexEntry = it.next();
				if (isExpired(indexEntry.getValue())) {
					it.remove();
				}
			}
			if (ns.isEmpty()) {
				emptyNamespaces.add(nsEntry.getKey());
			}
		}
		// Remove empty namespaces only if still empty (don't race with a fresh write).
		for (NamespaceKey ns : emptyNamespaces) {
			CACHE.computeIfPresent(ns, (k, m) -> m.isEmpty() ? null : m);
		}
	}

	private static long nowNanos() {
		return ticker.getAsLong();
	}

	private static String rsaSlot(int keySize) {
		return "rsa-" + keySize;
	}

	private static String ecSlot(Curve curve) {
		return "ec-" + curve.getName().toLowerCase();
	}

	private static String okpSlot(Curve curve) {
		return "okp-" + curve.getName().toLowerCase();
	}

	private static RSAKey generateRsa(int keySize) {
		try {
			return new RSAKeyGenerator(keySize).generate();
		} catch (JOSEException e) {
			throw new IllegalStateException("Failed to generate RSA-" + keySize, e);
		}
	}

	private static ECKey generateEc(Curve curve) {
		try {
			return new ECKeyGenerator(curve)
				.provider(BouncyCastleProviderSingleton.getInstance())
				.generate();
		} catch (JOSEException e) {
			throw new IllegalStateException("Failed to generate EC " + curve, e);
		}
	}

	private static OctetKeyPair generateOkp(Curve curve) {
		try {
			return new OctetKeyPairGenerator(curve).generate();
		} catch (JOSEException e) {
			throw new IllegalStateException("Failed to generate OKP " + curve, e);
		}
	}
}
