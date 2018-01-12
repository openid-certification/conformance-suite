/*******************************************************************************
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package io.fintechlabs.testframework.condition;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.fintechlabs.testframework.logging.LoggingRequestInterceptor;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public abstract class AbstractCondition implements Condition {
	
	private String testId;
	private TestInstanceEventLog log;
	private Set<String> requirements;
	private ConditionResult conditionResultOnFailure;
	
	protected AbstractCondition(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		this.testId = testId;
		this.log = log;
		this.conditionResultOnFailure = conditionResultOnFailure;
		this.requirements = Sets.newHashSet(requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.Condition#getMessage()
	 */
	public String getMessage() {
		return this.getClass().getSimpleName();
	}

	/*
	 * Logging utilities
	 */
	
	protected void log(JsonObject obj) {
		log.log(getMessage(), obj);
	}
	
	protected void log(String msg) {
		log.log(getMessage(), msg);
	}
	
	protected void log(Map<String, Object> map) {
		log.log(getMessage(), map);
	}
	
	protected void log(String msg, JsonObject in) {
		JsonObject copy = new JsonParser().parse(in.toString()).getAsJsonObject(); // don't modify the underlying object, round-trip to get a copy
		copy.addProperty("msg", msg);
		log(copy);
	}

	protected void log(String msg, Map<String, Object> map) {
		Map<String, Object> copy = new HashMap<>(map); // don't modify the underlying map
		copy.put("msg", msg);
		log(copy);
	}
	
	protected void logSuccess(JsonObject in) {
		JsonObject copy = new JsonParser().parse(in.toString()).getAsJsonObject(); // don't modify the underlying object, round-trip to get a copy
		copy.addProperty("result", ConditionResult.SUCCESS.toString());
		if (!getRequirements().isEmpty()) {
			JsonArray arr = new JsonArray();
			for (String req : getRequirements()) {
				arr.add(req);
			}
			copy.add("requirements", arr);
		}
		log(copy);
	}
	
	protected void logSuccess(String msg) {
		if (getRequirements().isEmpty()) {
			log(args("msg", msg, "result", ConditionResult.SUCCESS));
		} else {
			log(args("msg", msg, "result", ConditionResult.SUCCESS, "requirements", getRequirements()));
		}
	}
	
	protected void logSuccess(Map<String, Object> map) {
		Map<String, Object> copy = new HashMap<>(map); // don't modify the underlying map
		copy.put("result", ConditionResult.SUCCESS);
		if (!getRequirements().isEmpty()) {
			copy.put("requirements", getRequirements());
		}
		log(copy);
	}
	
	protected void logSuccess(String msg, JsonObject in) {
		JsonObject copy = new JsonParser().parse(in.toString()).getAsJsonObject(); // don't modify the underlying object, round-trip to get a copy
		copy.addProperty("msg", msg);
		copy.addProperty("result", ConditionResult.SUCCESS.toString());
		if (!getRequirements().isEmpty()) {
			JsonArray reqs = new JsonArray(getRequirements().size());
			for (String req : getRequirements()) {
				reqs.add(req);
			}
			copy.add("requirements", reqs);
		}
		log(copy);
	}

	protected void logSuccess(String msg, Map<String, Object> map) {
		Map<String, Object> copy = new HashMap<>(map); // don't modify the underlying map
		copy.put("msg", msg);
		copy.put("result", ConditionResult.SUCCESS);
		if (!getRequirements().isEmpty()) {
			copy.put("requirements", getRequirements());
		}
		log(copy);
	}
	
	/*
	 * Automatically log failures or warnings, depending on if this is an optional test
	 */
	
	protected void logFailure(JsonObject in) {
		JsonObject copy = new JsonParser().parse(in.toString()).getAsJsonObject(); // don't modify the underlying object, round-trip to get a copy
		copy.addProperty("result", conditionResultOnFailure.toString());
		if (!getRequirements().isEmpty()) {
			JsonArray arr = new JsonArray();
			for (String req : getRequirements()) {
				arr.add(req);
			}
			copy.add("requirements", arr);
		}
		log(copy);
	}
	
	protected void logFailure(String msg) {
		if (getRequirements().isEmpty()) {
			log(args("msg", msg, "result", conditionResultOnFailure));
		} else {
			log(args("msg", msg, "result", conditionResultOnFailure, "requirements", getRequirements()));
		}
	}
	
	protected void logFailure(Map<String, Object> map) {
		Map<String, Object> copy = new HashMap<>(map); // don't modify the underlying map
		copy.put("result", conditionResultOnFailure);
		if (!getRequirements().isEmpty()) {
			copy.put("requirements", getRequirements());
		}
		log(copy);
	}
	
	protected void logFailure(String msg, JsonObject in) {
		JsonObject copy = new JsonParser().parse(in.toString()).getAsJsonObject(); // don't modify the underlying object, round-trip to get a copy
		copy.addProperty("msg", msg);
		copy.addProperty("result", conditionResultOnFailure.toString());
		if (!getRequirements().isEmpty()) {
			JsonArray reqs = new JsonArray(getRequirements().size());
			for (String req : getRequirements()) {
				reqs.add(req);
			}
			copy.add("requirements", reqs);
		}
		log(copy);
	}

	protected void logFailure(String msg, Map<String, Object> map) {
		Map<String, Object> copy = new HashMap<>(map); // don't modify the underlying map
		copy.put("msg", msg);
		copy.put("result", conditionResultOnFailure);
		if (!getRequirements().isEmpty()) {
			copy.put("requirements", getRequirements());
		}
		log(copy);
	}

	/*
	 * Error utilities
	 */

	/**
	 * Log a failure then throw a ConditionError
	 */
	protected Environment error(String message, Throwable cause) {
		logFailure(message, ex(cause));
		throw new ConditionError(testId, getMessage() + ": " + message, cause);
	}

	/**
	 * Log a failure then throw a ConditionError
	 */
	protected Environment error(String message) {
		logFailure(message);
		throw new ConditionError(testId, getMessage() + ": " + message);
	}

	/**
	 * Log a failure then throw a ConditionError
	 */
	protected Environment error(Throwable cause) {
		logFailure(cause.getMessage(), ex(cause));
		throw new ConditionError(testId, getMessage(), cause);
	}
	
	/**
	 * Log a failure then throw a ConditionError
	 */
	protected Environment error(String message, Throwable cause, Map<String, Object> map) {
		logFailure(message, ex(cause, map));
		throw new ConditionError(testId, getMessage() + ": " + message, cause);
	}

	/**
	 * Log a failure then throw a ConditionError
	 */
	protected Environment error(String message, Map<String, Object> map) {
		logFailure(message, map);
		throw new ConditionError(testId, getMessage() + ": " + message);
	}

	/**
	 * Log a failure then throw a ConditionError
	 */
	protected Environment error(Throwable cause, Map<String, Object> map) {
		logFailure(cause.getMessage(), ex(cause, map));
		throw new ConditionError(testId, getMessage(), cause);
	}
	/**
	 * Log a failure then throw a ConditionError
	 */
	protected Environment error(String message, Throwable cause, JsonObject in) {
		logFailure(message, ex(cause, in));
		throw new ConditionError(testId, getMessage() + ": " + message, cause);
	}

	/**
	 * Log a failure then throw a ConditionError
	 */
	protected Environment error(String message, JsonObject in) {
		logFailure(message, in);
		throw new ConditionError(testId, getMessage() + ": " + message);
	}

	/**
	 * Log a failure then throw a ConditionError
	 */
	protected Environment error(Throwable cause, JsonObject in) {
		logFailure(cause.getMessage(), ex(cause, in));
		throw new ConditionError(testId, getMessage(), cause);
	}
	
	/**
	 * Get the list of requirements that this test would fulfill if it passed
	 * @return
	 */
	protected Set<String> getRequirements() {
		return requirements;
	}
	
	protected void createUploadPlaceholder(String msg) {
		if (getRequirements().isEmpty()) {
			log(msg, args("upload", RandomStringUtils.randomAlphanumeric(10), "result", ConditionResult.REVIEW));
		} else {
			log(msg, args("upload", RandomStringUtils.randomAlphanumeric(10), "result", ConditionResult.REVIEW, "requirements", getRequirements()));
		}
	}
	
	protected void createUploadPlaceholder() {
		if (getRequirements().isEmpty()) {
			log(args("upload", RandomStringUtils.randomAlphanumeric(10), "result", ConditionResult.REVIEW));
		} else {
			log(args("upload", RandomStringUtils.randomAlphanumeric(10), "result", ConditionResult.REVIEW, "requirements", getRequirements()));
		}
	}
	
	protected Map<String, Object> args(Object... a) {
		if (a == null || (a.length % 2) != 0) {
			throw new IllegalArgumentException("Need an even and nonzero number of arguments");
		}
		
		// start with an empty map of the right size
		HashMap<String, Object> m = new HashMap<>(a.length / 2);
		
		for (int i = 0; i < a.length; i += 2) {
			String key = (String) a[i];
			Object val = a[i + 1];
			m.put(key, val);
		}
		
		return m;
	}
	
		
	protected Map<String, Object> ex(Throwable cause) {
		return ex(cause, new HashMap<>());
	}
	
	protected Map<String, Object> ex(Throwable cause, Map<String, Object> in) {
		if (cause == null) {
			return null;
		}
		
		Map<String, Object> event = new HashMap<>(in);
		event.put("error", cause.getMessage());
		event.put("error_class", cause.getClass().getName());
		
		List<String> stack = Arrays.stream(cause.getStackTrace())
			.map(StackTraceElement::toString)
			.collect(Collectors.toList());

		event.put("stacktrace", stack);

		return event;
	}
	
	protected JsonObject ex(Throwable cause, JsonObject in) {
		JsonObject copy = new JsonParser().parse(in.toString()).getAsJsonObject(); // don't modify the underlying object, round-trip to get a copy
		copy.addProperty("error", cause.getMessage());
		copy.addProperty("error_class", cause.getClass().getName());
		
		JsonArray stack = Arrays.stream(cause.getStackTrace())
			.map(StackTraceElement::toString)
			.collect(() -> new JsonArray(cause.getStackTrace().length),
					(c, e) -> c.add(e),
					(c1, c2) -> c1.addAll(c2));
		
		copy.add("stacktrace", stack);

		return copy;
	}
	
	
	/*
	 * Create an HTTP Client for use in calling outbound to other services
	 */
	protected HttpClient createHttpClient(Environment env) throws CertificateException, InvalidKeySpecException, NoSuchAlgorithmException, KeyStoreException, IOException, UnrecoverableKeyException, KeyManagementException {
		HttpClientBuilder builder = HttpClientBuilder.create()
				.useSystemProperties();
	
		KeyManager[] km = null;

		// initialize MTLS if it's available
		if (env.containsObj("mutual_tls_authentication")) {
	
			// TODO: move this to an extractor?
			String clientCert = env.getString("mutual_tls_authentication", "cert");
			String clientKey = env.getString("mutual_tls_authentication", "key");
			String clientCa = env.getString("mutual_tls_authentication", "ca");
	
			byte[] certBytes = Base64.getDecoder().decode(clientCert);
			byte[] keyBytes = Base64.getDecoder().decode(clientKey);
	
			X509Certificate cert = generateCertificateFromDER(certBytes);              
			RSAPrivateKey key  = generatePrivateKeyFromDER(keyBytes);
	
			ArrayList<X509Certificate> chain = Lists.newArrayList(cert);
			if (clientCa != null) {
				byte[] caBytes = Base64.getDecoder().decode(clientCa);
				chain.addAll(generateCertificateChainFromDER(caBytes));
			}

			KeyStore keystore = KeyStore.getInstance("JKS");
			keystore.load(null);
			keystore.setCertificateEntry("cert-alias", cert);
			keystore.setKeyEntry("key-alias", key, "changeit".toCharArray(), chain.toArray(new Certificate[chain.size()]));
	
			KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			keyManagerFactory.init(keystore, "changeit".toCharArray());
	
			km = keyManagerFactory.getKeyManagers();
	
		}
	
		TrustManager[] trustAllCerts = new TrustManager[] {
				new X509TrustManager() {

					@Override
					public X509Certificate[] getAcceptedIssuers() {
						return new X509Certificate[0];
					}

					@Override
					public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
					}

					@Override
					public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
					}
				}
		};

		SSLContext sc = SSLContext.getInstance("TLS");
		sc.init(km, trustAllCerts, new java.security.SecureRandom());

		builder.setSslcontext(sc);

		SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(sc,
				new String[]{"TLSv1.2"},
				null,
				SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

		builder.setSSLSocketFactory(sslConnectionSocketFactory);

		Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
				.register("https", sslConnectionSocketFactory)
				.register("http", new PlainConnectionSocketFactory())
				.build();

		HttpClientConnectionManager ccm = new BasicHttpClientConnectionManager(registry);
		builder.setConnectionManager(ccm);
	
		HttpClient httpClient = builder.build();
		return httpClient;
	}

	protected RestTemplate createRestTemplate(Environment env) throws UnrecoverableKeyException, KeyManagementException, CertificateException, InvalidKeySpecException, NoSuchAlgorithmException, KeyStoreException, IOException {
		HttpClient httpClient = createHttpClient(env);
	
		RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));

		restTemplate.getInterceptors().add(new LoggingRequestInterceptor(getMessage(), log));

		return restTemplate;
	}

	protected static RSAPrivateKey generatePrivateKeyFromDER(byte[] keyBytes) throws InvalidKeySpecException, NoSuchAlgorithmException {
		PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
	
		KeyFactory factory = KeyFactory.getInstance("RSA");
	
		return (RSAPrivateKey)factory.generatePrivate(spec);        
	}

	protected static X509Certificate generateCertificateFromDER(byte[] certBytes) throws CertificateException {
		CertificateFactory factory = CertificateFactory.getInstance("X.509");
	
		return (X509Certificate)factory.generateCertificate(new ByteArrayInputStream(certBytes));      
	}

	protected static List<X509Certificate> generateCertificateChainFromDER(byte[] chainBytes) throws CertificateException {
		CertificateFactory factory = CertificateFactory.getInstance("X.509");

		ArrayList<X509Certificate> chain = new ArrayList<X509Certificate>();
		ByteArrayInputStream in = new ByteArrayInputStream(chainBytes);
		while (in.available() > 0) {
			chain.add((X509Certificate) factory.generateCertificate(in));
		}

		return chain;
	}

	
}
