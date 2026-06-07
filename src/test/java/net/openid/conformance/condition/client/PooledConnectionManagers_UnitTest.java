package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.Environment;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PooledConnectionManagers_UnitTest {

	@BeforeEach
	@AfterEach
	void reset() {
		PooledConnectionManagers.clear();
	}

	private Environment envWith(Boolean cacheFlag, String cert, String key, String ca) {
		Environment env = new Environment();
		if (cacheFlag != null) {
			JsonObject options = new JsonObject();
			options.addProperty("cache_external_metadata", cacheFlag);
			JsonObject config = new JsonObject();
			config.add("options", options);
			env.putObject("config", config);
		}
		if (cert != null) {
			JsonObject mtls = new JsonObject();
			mtls.addProperty("cert", cert);
			mtls.addProperty("key", key);
			if (ca != null) {
				mtls.addProperty("ca", ca);
			}
			env.putObject("mutual_tls_authentication", mtls);
		}
		return env;
	}

	@Test
	void isEnabled_reflectsTheCacheFlag() {
		assertTrue(PooledConnectionManagers.isEnabled(envWith(true, null, null, null)));
		assertFalse(PooledConnectionManagers.isEnabled(envWith(false, null, null, null)));
		assertFalse(PooledConnectionManagers.isEnabled(new Environment()), "no config object → not enabled");
	}

	@Test
	void identityKey_sameMaterialProducesSameKey() {
		assertEquals(
			PooledConnectionManagers.identityKey(envWith(true, "CERT", "KEY", "CA"), true),
			PooledConnectionManagers.identityKey(envWith(true, "CERT", "KEY", "CA"), true));
	}

	@Test
	void identityKey_differsByCertKeyCaAndTlsRestriction() {
		String base = PooledConnectionManagers.identityKey(envWith(true, "C", "K", "A"), true);
		assertNotEquals(base, PooledConnectionManagers.identityKey(envWith(true, "C2", "K", "A"), true), "cert is part of identity");
		assertNotEquals(base, PooledConnectionManagers.identityKey(envWith(true, "C", "K2", "A"), true), "private key is part of identity");
		assertNotEquals(base, PooledConnectionManagers.identityKey(envWith(true, "C", "K", "A2"), true), "CA chain is part of identity");
		assertNotEquals(base, PooledConnectionManagers.identityKey(envWith(true, "C", "K", "A"), false), "TLS-version restriction is part of identity");
	}

	@Test
	void identityKey_noMtlsIsStableAndDistinctFromMtls() {
		String noMtls = PooledConnectionManagers.identityKey(envWith(true, null, null, null), true);
		assertEquals(noMtls, PooledConnectionManagers.identityKey(envWith(true, null, null, null), true));
		assertNotEquals(noMtls, PooledConnectionManagers.identityKey(envWith(true, "C", "K", "A"), true));
	}

	@Test
	void getOrCreate_reusesPerIdentityAndIsolatesAcrossIdentities() {
		AtomicInteger built = new AtomicInteger();
		Supplier<PoolingHttpClientConnectionManager> factory = () -> {
			built.incrementAndGet();
			return new PoolingHttpClientConnectionManager();
		};
		HttpClientConnectionManager a1 = PooledConnectionManagers.getOrCreate("id-A", factory);
		HttpClientConnectionManager a2 = PooledConnectionManagers.getOrCreate("id-A", factory);
		HttpClientConnectionManager b1 = PooledConnectionManagers.getOrCreate("id-B", factory);

		assertSame(a1, a2, "same identity reuses the same manager");
		assertNotSame(a1, b1, "different identity → different manager (no cross-identity leak)");
		assertEquals(2, built.get(), "factory invoked once per distinct identity");
		assertEquals(2, PooledConnectionManagers.identityCount());
	}

	@Test
	void evict_removesIdentityAndNextGetRebuilds() {
		PooledConnectionManagers.getOrCreate("id-A", PoolingHttpClientConnectionManager::new);
		PooledConnectionManagers.getOrCreate("id-B", PoolingHttpClientConnectionManager::new);
		assertEquals(2, PooledConnectionManagers.identityCount());

		PooledConnectionManagers.evict("id-A");
		assertEquals(1, PooledConnectionManagers.identityCount());

		HttpClientConnectionManager rebuilt = PooledConnectionManagers.getOrCreate("id-A", PoolingHttpClientConnectionManager::new);
		assertNotEquals(0, PooledConnectionManagers.identityCount());
		assertEquals(2, PooledConnectionManagers.identityCount());
		assertTrue(rebuilt != null);
	}

	@Test
	void clear_dropsEveryIdentity() {
		PooledConnectionManagers.getOrCreate("id-A", PoolingHttpClientConnectionManager::new);
		PooledConnectionManagers.getOrCreate("id-B", PoolingHttpClientConnectionManager::new);
		PooledConnectionManagers.clear();
		assertEquals(0, PooledConnectionManagers.identityCount());
	}
}
