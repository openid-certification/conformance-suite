package net.openid.conformance.condition.util;

import net.openid.conformance.testmodule.Environment;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.cache.CacheConfig;
import org.apache.hc.client5.http.impl.cache.CachingHttpClientBuilder;
import org.apache.hc.client5.http.impl.cache.HttpByteArrayCacheEntrySerializer;
import org.apache.hc.client5.http.impl.cache.ehcache.EhcacheHttpCacheStorage;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.BasicHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.TlsSocketStrategy;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.spi.loaderwriter.CacheLoaderWriter;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;

public class HttpClientBuilderFactory {

	private static final TrustManager[] trustAllCerts = {new X509TrustAll()};

	private static final HttpClientBuilder sharedHttpClientBuilder;

	static HttpClientBuilder sharedCachedHttpClientBuilder = null;

	static Cache<String, byte[]> httpCache;
	static EhcacheHttpCacheStorage<byte[]> httpCacheStorage;
	static CacheConfig cfgCache = CacheConfig.custom().setHeuristicCachingEnabled(true).setHeuristicDefaultLifetime(TimeValue.of(Duration.ofMinutes(2))).build();

	static {
		KeyManager[] km = null;

		SSLContext sc;
		try {
			sc = SSLContext.getInstance("TLS");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		try {
			sc.init(km, trustAllCerts, new java.security.SecureRandom());
		} catch (KeyManagementException e) {
			throw new RuntimeException(e);
		}

		TlsSocketStrategy tlsStrategy = (TlsSocketStrategy) ClientTlsStrategyBuilder.create()
			.setSslContext(sc)
			.setTlsVersions("TLSv1.2", "TLSv1.3")
			.setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
			.build();

		HttpClientBuilder builder = HttpClientBuilder.create().useSystemProperties();
		builder.setDefaultRequestConfig(RequestConfig.custom().build());

		PoolingHttpClientConnectionManager ccm = PoolingHttpClientConnectionManagerBuilder.create()
			.setTlsSocketStrategy(tlsStrategy)
			.build();

		int timeout = 60;
		ccm.setDefaultConnectionConfig(ConnectionConfig.custom()
				.setConnectTimeout(Timeout.ofSeconds(timeout))
				.setSocketTimeout(Timeout.ofSeconds(timeout))
				.setTimeToLive(Timeout.ofSeconds(5))
				.build());
		builder.setConnectionManager(ccm);
		builder.disableRedirectHandling();
		builder.disableAutomaticRetries();

		sharedHttpClientBuilder = builder;


		CacheConfiguration<String, byte[]> cacheConfiguration = CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, byte[].class,
						ResourcePoolsBuilder.heap(1000))
				.build();

		CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
				.build(true);

		httpCache = cacheManager.createCache("discovery_cache", cacheConfiguration);


		httpCacheStorage = new EhcacheHttpCacheStorage<>(httpCache, cfgCache,new HttpByteArrayCacheEntrySerializer());


		sharedCachedHttpClientBuilder = CachingHttpClientBuilder.create().setCacheConfig(cfgCache).setHttpCacheStorage(httpCacheStorage).useSystemProperties();
		sharedCachedHttpClientBuilder.setDefaultRequestConfig(RequestConfig.custom().build());
		sharedCachedHttpClientBuilder.setConnectionManager(ccm);
		sharedCachedHttpClientBuilder.disableRedirectHandling();
		sharedCachedHttpClientBuilder.disableAutomaticRetries();

	}

	public static HttpClientBuilder createSharedHttpClientBuilder() {
		return sharedHttpClientBuilder;
	}

	public static HttpClientBuilder createSharedCacheableHttpClientBuilder() {
		return sharedCachedHttpClientBuilder;
	}

	static class ComparableEnvironment {
		private final Environment env;
		public ComparableEnvironment(Environment env){
			this.env = env;
		}

		public Environment getEnv(){
			return this.env;
		}

		@Override
		public boolean equals(Object that) {
			String thisClientCert = this.env.getString("mutual_tls_authentication", "cert");
			String thatClientCert = ((ComparableEnvironment)that).env.getString("mutual_tls_authentication", "cert");
			return thisClientCert.equals(thatClientCert);
		}

		@Override
		public int hashCode() {
			return env.getString("mutual_tls_authentication", "cert").hashCode();
		}
	}
	static Cache<ComparableEnvironment, KeyManager[]> clientMtlsCache;

	static {
		CacheConfiguration<ComparableEnvironment, KeyManager[]> cacheConfiguration = CacheConfigurationBuilder.newCacheConfigurationBuilder(ComparableEnvironment.class, KeyManager[].class,
						ResourcePoolsBuilder.heap(1000))
				.withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofMinutes(10)))
				.withLoaderWriter(new CacheLoaderWriter<>() {
					@Override
					public KeyManager[] load(ComparableEnvironment env) throws Exception {
						return MtlsKeystoreBuilder.configureMtls(env.getEnv());
					}

					@Override
					public void write(ComparableEnvironment key, KeyManager[] value) throws Exception {

					}

					@Override
					public void delete(ComparableEnvironment key) throws Exception {

					}
				})
				.build();

		CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
				.build(true);

		clientMtlsCache = cacheManager.createCache("client_mtls", cacheConfiguration);

	}

	public static HttpClientBuilder createMtlsHttpClientBuilder(Environment env) throws KeyManagementException {
		KeyManager[] km = clientMtlsCache.get(new ComparableEnvironment(env));


		SSLContext sc;
		try {
			sc = SSLContext.getInstance("TLS");
		} catch (NoSuchAlgorithmException e) {
			//it would happen only if there is a serious error on the deployment
			throw new RuntimeException(e);
		}
		sc.init(km, trustAllCerts, new java.security.SecureRandom());

		TlsSocketStrategy tlsStrategy = (TlsSocketStrategy) ClientTlsStrategyBuilder.create()
			.setSslContext(sc)
			.setTlsVersions("TLSv1.2", "TLSv1.3")
			.setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
			.build();

		HttpClientBuilder builder = HttpClientBuilder.create().useSystemProperties();
		builder.setDefaultRequestConfig(RequestConfig.custom().build());

		Registry<TlsSocketStrategy> registry = RegistryBuilder.<TlsSocketStrategy>create()
			.register("https", tlsStrategy)
			.build();

		BasicHttpClientConnectionManager ccm = BasicHttpClientConnectionManager.create(registry);
		int timeout = 60; // seconds
		ccm.setConnectionConfig(ConnectionConfig.custom()
				.setConnectTimeout(Timeout.ofSeconds(timeout))
				.setSocketTimeout(Timeout.ofSeconds(timeout))
				.setTimeToLive(Timeout.ofSeconds(timeout))
				.build());

		builder.setConnectionManager(ccm);

		builder.disableRedirectHandling();

		builder.disableAutomaticRetries();

		return builder;
	}
}
