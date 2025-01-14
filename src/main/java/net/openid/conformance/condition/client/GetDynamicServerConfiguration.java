package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.apache.hc.client5.http.impl.cache.CacheConfig;
import org.apache.hc.client5.http.impl.cache.CachingHttpClientBuilder;
import org.apache.hc.client5.http.impl.cache.HttpByteArrayCacheEntrySerializer;
import org.apache.hc.core5.util.TimeValue;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import org.apache.hc.client5.http.impl.cache.ehcache.EhcacheHttpCacheStorage;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.time.Duration;

public class GetDynamicServerConfiguration extends AbstractCondition {

	static Cache<String, byte[]> discoveryCache;
	static EhcacheHttpCacheStorage<byte[]> cacheStorage;
	static CacheConfig cfgCache = CacheConfig.custom().setHeuristicCachingEnabled(true).setHeuristicDefaultLifetime(TimeValue.of(Duration.ofMinutes(2))).build();
	static {
		CacheConfiguration<String, byte[]> cacheConfiguration = CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, byte[].class,
						ResourcePoolsBuilder.heap(1000))
				.build();

		CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
				.build(true);

		discoveryCache = cacheManager.createCache("discovery_cache", cacheConfiguration);


		cacheStorage = new EhcacheHttpCacheStorage<>(discoveryCache, cfgCache,new HttpByteArrayCacheEntrySerializer());

	}


	@Override
	@PreEnvironment(required = "config")
	@PostEnvironment(required = { "server", "discovery_endpoint_response" } )
	public Environment evaluate(Environment env) {

		if (!env.containsObject("config")) {
			throw error("Couldn't find a configuration");
		}

		String staticIssuer = env.getString("config", "server.issuer");

		if (!Strings.isNullOrEmpty(staticIssuer)) {
			throw error("Test set to use dynamic server configuration but test configuration contains static server configuration", args("issuer", staticIssuer));
		}

		String discoveryUrl = env.getString("config", "server.discoveryUrl");

		if (Strings.isNullOrEmpty(discoveryUrl)) {

			String iss = env.getString("config", "server.discoveryIssuer");
			discoveryUrl = iss + "/.well-known/openid-configuration";

			if (Strings.isNullOrEmpty(iss)) {
				throw error("Couldn't find discoveryUrl or discoveryIssuer field for discovery purposes");
			}

		}

		// get out the server configuration component
		if (Strings.isNullOrEmpty(discoveryUrl)) {
			throw error("Couldn't find or construct a discovery URL");
		}



		// fetch the value
		String jsonString;
		try {
			RestTemplate restTemplate = createRestTemplate(CachingHttpClientBuilder.create().setCacheConfig(cfgCache).setHttpCacheStorage(cacheStorage).build(), null);
			ResponseEntity<String> response = restTemplate.exchange(discoveryUrl, HttpMethod.GET, null, String.class);
			JsonObject responseInfo = convertResponseForEnvironment("discovery", response);

			env.putObject("discovery_endpoint_response", responseInfo);

			jsonString = response.getBody();
		} catch (UnrecoverableKeyException | KeyManagementException | CertificateException | InvalidKeySpecException | NoSuchAlgorithmException | KeyStoreException | IOException e) {
			throw error("Error creating HTTP client", e);
		} catch (RestClientException e) {
			String msg = "Unable to fetch server configuration from " + discoveryUrl;
			if (e.getCause() != null) {
				msg += " - " +e.getCause().getMessage();
			}
			throw error(msg, e);
		}

		if (!Strings.isNullOrEmpty(jsonString)) {
			try {
				JsonObject serverConfig = JsonParser.parseString(jsonString).getAsJsonObject();

				logSuccess("Successfully parsed server configuration", serverConfig);

				env.putObject("server", serverConfig);

				return env;
			} catch (JsonSyntaxException e) {
				throw error(e, args("json", jsonString));
			}

		} else {
			throw error("empty server configuration");
		}



	}

}
