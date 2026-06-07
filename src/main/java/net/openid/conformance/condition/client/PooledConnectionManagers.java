package net.openid.conformance.condition.client;

import net.openid.conformance.testmodule.Environment;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Process-wide registry of {@link PoolingHttpClientConnectionManager}s, one per TLS client identity,
 * so conditions can reuse TCP/TLS (and mTLS) connections to the same OP instead of doing a fresh
 * handshake on every call. Opt-in: only used by {@link net.openid.conformance.condition.AbstractCondition}
 * when a test config sets {@code "options": {"cache_external_metadata": true}} (the same flag that
 * enables the metadata cache), so pooling is confined to the controlled CI configs.
 *
 * <p><b>Why keyed by full TLS identity.</b> A previous pooling attempt (MR !1551, reverted in !1573,
 * issue #1466) cached the mTLS {@code KeyManager[]} keyed on the leaf client cert <em>only</em>, while
 * the cached value carried the CA <em>chain</em> presented in the handshake. Under concurrent tests a
 * stale/raced entry presented the wrong (or, with a null KeyManager, no) chain — Authlete's
 * {@code Path does not chain with any of the trust anchors}. Here the key is a hash of the full
 * identity (cert + key + ca + the TLS-version restriction), computed fresh per call from immutable
 * snapshot strings (never holding the mutable {@link Environment}), so a connection opened with one
 * client identity can never be leased to a request needing a different one. The caller must also
 * never build a no-cert manager for an mTLS identity (see AbstractCondition's null-guard).
 *
 * <p><b>Stale-connection handling.</b> Observed servers (e.g. Authlete/Jetty) advertise
 * {@code Keep-Alive: timeout=5} and close an idle connection after ~5s. FAPI/CIBA flows routinely pause
 * longer than that (browser auth between PAR and token, CIBA poll sleeps), so a connection left idle
 * across such a gap is dead before it would be reused, and reusing it fails with "Socket closed". To
 * avoid that we (a) proactively evict connections idle longer than {@link #IDLE_EVICT_SECONDS}s — below
 * the server's 5s — via a background sweep, so a connection is never offered for reuse in the danger
 * window, and (b) revalidate any connection idle beyond {@link #VALIDATE_AFTER_INACTIVITY} before
 * leasing, as a cheap backstop. Back-to-back calls (under a couple of seconds apart) still reuse the
 * connection and save the handshake.
 */
public final class PooledConnectionManagers {

	/** Short connection lifetime so a pooled connection can't be reused long after the test that
	 *  opened it. Long enough to reuse across one test's back-to-back calls. */
	private static final Timeout CONNECTION_TTL = Timeout.ofSeconds(30);
	/** Revalidate (stale-check) a connection idle beyond this before leasing it. Kept well below the
	 *  observed ~5s server keep-alive timeout so connections the server has closed are caught. */
	private static final Timeout VALIDATE_AFTER_INACTIVITY = Timeout.ofSeconds(1);
	/** Proactively close connections idle longer than this (below the observed ~5s server keep-alive
	 *  timeout), so a server-closed connection is never offered for reuse. */
	private static final long IDLE_EVICT_SECONDS = 4;
	/** How often the background sweep evicts idle/expired connections. */
	private static final long EVICT_SWEEP_SECONDS = 1;
	private static final int MAX_TOTAL = 20;
	private static final int MAX_PER_ROUTE = 10;
	/** Bound on the number of distinct identities pooled (defensive; opt-in CI configs have a handful). */
	private static final int MAX_IDENTITIES = 128;

	private static final Map<String, PoolingHttpClientConnectionManager> MANAGERS = new ConcurrentHashMap<>();
	private static volatile ScheduledExecutorService idleEvictor;

	private PooledConnectionManagers() {}

	/** True when the test config opts into connection reuse (rides the metadata-cache opt-in flag). */
	public static boolean isEnabled(Environment env) {
		return Boolean.TRUE.equals(env.getBoolean("config", "options.cache_external_metadata"));
	}

	/**
	 * Stable key for the full TLS client identity of this request. mTLS requests are keyed by a hash of
	 * cert+key+ca (the private key is hashed, never stored/logged); non-mTLS requests share one key. The
	 * TLS-version restriction is part of the key because it changes the socket factory.
	 */
	public static String identityKey(Environment env, boolean restrictTLSVersions) {
		String suffix = restrictTLSVersions ? "|tls12+" : "|tlsAny";
		if (!env.containsObject("mutual_tls_authentication")) {
			return "no-mtls" + suffix;
		}
		String cert = env.getString("mutual_tls_authentication", "cert");
		String key = env.getString("mutual_tls_authentication", "key");
		String ca = env.getString("mutual_tls_authentication", "ca");
		return "mtls:" + sha256(cert + "\n" + key + "\n" + ca) + suffix;
	}

	/** Get (or build once) the shared pooled connection manager for {@code identityKey}. */
	public static HttpClientConnectionManager getOrCreate(String identityKey,
			Supplier<PoolingHttpClientConnectionManager> factory) {
		ensureIdleEvictor();
		if (MANAGERS.size() >= MAX_IDENTITIES && !MANAGERS.containsKey(identityKey)) {
			// Defensive cap: drop one existing identity rather than grow unbounded.
			MANAGERS.keySet().stream().findFirst().ifPresent(PooledConnectionManagers::evict);
		}
		return MANAGERS.computeIfAbsent(identityKey, k -> factory.get());
	}

	/**
	 * Build a pooling manager over {@code registry} (which carries this identity's SSL socket factory),
	 * with the standard timeouts plus a short connection TTL.
	 */
	@SuppressWarnings("deprecation")
	public static PoolingHttpClientConnectionManager newManager(Registry<ConnectionSocketFactory> registry, int timeoutSeconds) {
		PoolingHttpClientConnectionManager pm = new PoolingHttpClientConnectionManager(registry);
		pm.setDefaultConnectionConfig(ConnectionConfig.custom()
			.setConnectTimeout(Timeout.ofSeconds(timeoutSeconds))
			.setSocketTimeout(Timeout.ofSeconds(timeoutSeconds))
			.setTimeToLive(CONNECTION_TTL)
			.setValidateAfterInactivity(VALIDATE_AFTER_INACTIVITY)
			.build());
		// Also set it directly on the manager, not only via the default ConnectionConfig, so the
		// stale-check is applied regardless of how the manager resolves per-route config.
		pm.setValidateAfterInactivity(VALIDATE_AFTER_INACTIVITY);
		pm.setMaxTotal(MAX_TOTAL);
		pm.setDefaultMaxPerRoute(MAX_PER_ROUTE);
		return pm;
	}

	/**
	 * Lazily start a single shared daemon sweep that proactively closes idle/expired connections across
	 * all pooled managers, so a connection the server has already closed (after its keep-alive timeout)
	 * is evicted before it can be leased for reuse. Started only once pooling is actually used.
	 */
	@SuppressWarnings("FutureReturnValueIgnored") // fire-and-forget periodic sweep; no handle needed
	private static synchronized void ensureIdleEvictor() {
		if (idleEvictor == null) {
			ScheduledExecutorService evictor = Executors.newSingleThreadScheduledExecutor(r -> {
				Thread t = new Thread(r, "pooled-conn-idle-evictor");
				t.setDaemon(true);
				return t;
			});
			evictor.scheduleWithFixedDelay(PooledConnectionManagers::evictIdleConnections,
				EVICT_SWEEP_SECONDS, EVICT_SWEEP_SECONDS, TimeUnit.SECONDS);
			idleEvictor = evictor;
		}
	}

	private static void evictIdleConnections() {
		for (PoolingHttpClientConnectionManager m : MANAGERS.values()) {
			try {
				m.closeExpired();
				m.closeIdle(TimeValue.ofSeconds(IDLE_EVICT_SECONDS));
			} catch (RuntimeException e) {
				// Best-effort background sweep: never let one manager's failure stop the others.
			}
		}
	}

	/**
	 * Close and forget the manager for {@code identityKey}, aborting any in-flight requests on its
	 * connections. Used as the hard-deadline abort action for pooled requests (closing the shared
	 * manager is how a stuck pooled request gets aborted; the next request rebuilds the pool).
	 */
	public static void evict(String identityKey) {
		PoolingHttpClientConnectionManager m = MANAGERS.remove(identityKey);
		if (m != null) {
			m.close(CloseMode.IMMEDIATE);
		}
	}

	/** Drop and close every pooled manager. Intended for test isolation. */
	public static void clear() {
		MANAGERS.keySet().forEach(PooledConnectionManagers::evict);
	}

	/** For tests. */
	static int identityCount() {
		return MANAGERS.size();
	}

	private static String sha256(String s) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(md.digest(s.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 unavailable", e);
		}
	}
}
