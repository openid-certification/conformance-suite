package net.openid.conformance.condition.util;

import net.openid.conformance.testmodule.Environment;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.BasicHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
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

	static TrustManager[] trustAllCerts = {new X509TrustAll()};

	static HttpClientBuilder sharedHttpClientBuilder = null;

	static {
		KeyManager[] km = null;

		SSLContext sc = null;
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

		SSLConnectionSocketFactory sslConnectionFactory = SSLConnectionSocketFactoryBuilder.create()
				.setSslContext(sc)
				.setTlsVersions( new String[]{"TLSv1.2", "TLSv1.3"})
				.setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
				.build();

		HttpClientBuilder builder = HttpClientBuilder.create().useSystemProperties();
		builder.setDefaultRequestConfig(RequestConfig.custom().build());

		Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
				.register("https", sslConnectionFactory)
				.register("http", new PlainConnectionSocketFactory())
				.build();

		PoolingHttpClientConnectionManager ccm = new PoolingHttpClientConnectionManager(registry);

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
	}

	public static HttpClientBuilder createSharedHttpClientBuilder() {
		return sharedHttpClientBuilder;
	}


	static class ComparableEnvironment {
		private Environment env;
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


		SSLContext sc = null;
		try {
			sc = SSLContext.getInstance("TLS");
		} catch (NoSuchAlgorithmException e) {
			//it would happen only if there is a serious error on the deployment
			throw new RuntimeException(e);
		}
		sc.init(km, trustAllCerts, new java.security.SecureRandom());

		SSLConnectionSocketFactory sslConnectionFactory = SSLConnectionSocketFactoryBuilder.create()
				.setSslContext(sc)
				.setTlsVersions(new String[]{"TLSv1.2", "TLSv1.3"} )
				.setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
				.build();

		HttpClientBuilder builder = HttpClientBuilder.create().useSystemProperties();
		builder.setDefaultRequestConfig(RequestConfig.custom().build());

		Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
				.register("https", sslConnectionFactory)
				.register("http", new PlainConnectionSocketFactory())
				.build();

		BasicHttpClientConnectionManager ccm = new BasicHttpClientConnectionManager(registry);
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
