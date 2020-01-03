package net.openid.conformance.condition;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.Socket;
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
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import net.openid.conformance.logging.LoggingRequestInterceptor;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.openid.conformance.testmodule.DataUtils;

public abstract class AbstractCondition implements Condition, DataUtils {

	private static final Logger logger = LoggerFactory.getLogger(AbstractCondition.class);

	private String testId;
	private TestInstanceEventLog log;
	private Set<String> requirements;
	private ConditionResult conditionResultOnFailure;
	private boolean logged = false;

	public void setProperties(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		this.testId = testId;
		this.log = log;
		this.conditionResultOnFailure = conditionResultOnFailure;
		this.requirements = Sets.newHashSet(requirements);
	}

	@Override
	public void execute(Environment env) {
		try {
			Method eval = this.getClass().getMethod("evaluate", Environment.class);
			PreEnvironment pre = eval.getAnnotation(PreEnvironment.class);
			if (pre != null) {
				for (String req : pre.required()) {
					if (!env.containsObject(req)) {
						logger.info("[pre] Test condition " + this.getClass().getSimpleName() + " failure, couldn't find object in environment: " + req);
						log.log(this.getMessage(), args(
							"msg", "Condition failure, couldn't find required object in environment before evaluation: " + req,
							"expected", req,
							"result", ConditionResult.FAILURE,
							"mapped", env.isKeyShadowed(req) ? env.getEffectiveKey(req) : null,
							"requirements", this.getRequirements()
							// TODO: log the environment here?
						));
						throw alreadyLoggedPrePostError("[pre] Couldn't find object in environment: " + req);
					}
				}
				for (String s : pre.strings()) {
					if (env.getString(s) == null) {
						logger.info("[pre] Test condition " + this.getClass().getSimpleName() + " failure, couldn't find string in environment: " + s);
						log.log(this.getMessage(), args(
							"msg", "Condition failure, couldn't find required string in environment before evaluation: " + s,
							"expected", s,
							"result", ConditionResult.FAILURE,
							"requirements", this.getRequirements()
							// TODO: log the environment here?
						));
						throw alreadyLoggedPrePostError("[pre] Couldn't find string in environment: " + s);
					}
				}
			}

			// evaluate the condition and assign its results back to our environment
			env = evaluate(env);
			if (!logged) {
				log.log(this.getMessage(),
					args("msg", "Condition ran but did not log anything"));
			}

			// check the environment to make sure the condition did what it claimed to
			PostEnvironment post = eval.getAnnotation(PostEnvironment.class);
			if (post != null) {
				for (String req : post.required()) {
					if (!env.containsObject(req)) {
						logger.info("[post] Test condition " + this.getClass().getSimpleName() + " failure, couldn't find object in environment: " + req);
						log.log(this.getMessage(), args(
							"msg", "Condition failure, couldn't find required object in environment after evaluation: " + req,
							"expected", req,
							"result", ConditionResult.FAILURE,
							"mapped", env.isKeyShadowed(req) ? env.getEffectiveKey(req) : null,
							"requirements", this.getRequirements()
							// TODO: log the environment here?
						));
						throw alreadyLoggedPrePostError("[post] Couldn't find object in environment: " + req);
					}
				}
				for (String s : post.strings()) {
					if (env.getString(s) == null) {
						logger.info("[post] Test condition " + this.getClass().getSimpleName() + " failure, couldn't find string in environment: " + s);
						log.log(this.getMessage(), args(
							"msg", "Condition failure, couldn't find required string in environment after evaluation: " + s,
							"expected", s,
							"result", ConditionResult.FAILURE,
							"requirements", this.getRequirements()
							// TODO: log the environment here?
						));
						throw alreadyLoggedPrePostError("[post] Couldn't find string in environment: " + s);
					}
				}
			}
		} catch (NoSuchMethodException e) {
			logger.error("Couldn't create condition object", e);
			log.log(this.getMessage(), args(
				"msg", "Condition failure, couldn't get 'evaluate' method for condition '" + this.getClass().getSimpleName() + "'",
				"result", ConditionResult.FAILURE
			));
			throw alreadyLoggedPrePostError("Couldn't get 'evaluate' method for condition '" + this.getClass().getSimpleName() + "'", e);
		}
	}

	/**
	 * Tests if the condition holds true. Reads from the given environment and returns a potentially modified environment.
	 *
	 * Throws ConditionError when condition isn't met.
	 *
	 * Decorate with @PreEnvironment to ensure objects or strings are in the environment before evaluation.
	 * Decorate with @PostEnvironment to ensure objects or strings are in the environment after evaluation.
	 */
	protected abstract Environment evaluate(Environment env);

	/**
	 * Get the testId for this instance
	 * @return The test Id for the current instance
	 */
	protected String getTestId() {
		return this.testId;
	}

	/*
	 * Logging utilities
	 */

	protected void log(JsonObject obj) {
		log.log(getMessage(), obj);
		logged = true;
	}

	protected void log(String msg) {
		log.log(getMessage(), msg);
		logged = true;
	}

	protected void log(Map<String, Object> map) {
		log.log(getMessage(), map);
		logged = true;
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
	 * Return a ConditionError for failures in the Pre/Post Environment annotations
	 */
	private ConditionError alreadyLoggedPrePostError(String message, Throwable cause) {
		// it is assumed the caller has already written an entry to the event log
		return new ConditionError(testId, getMessage() + ": " + message, true, cause);
	}

	/**
	 * Return a ConditionError for failures in the Pre/Post Environment annotations
	 */
	private ConditionError alreadyLoggedPrePostError(String message) {
		// it is assumed the caller has already written an entry to the event log
		return new ConditionError(testId, getMessage() + ": " + message, true);
	}

	/**
	 * Log a failure then return a ConditionError
	 */
	protected ConditionError error(String message, Throwable cause) {
		logFailure(message, ex(cause));
		return new ConditionError(testId, getMessage() + ": " + message, cause);
	}

	/**
	 * Log a failure then return a ConditionError
	 */
	protected ConditionError error(String message) {
		logFailure(message);
		return new ConditionError(testId, getMessage() + ": " + message);
	}

	/**
	 * Log a failure then return a ConditionError
	 */
	protected ConditionError error(Throwable cause) {
		logFailure(cause.getMessage(), ex(cause));
		return new ConditionError(testId, getMessage(), cause);
	}

	/**
	 * Log a failure then return a ConditionError
	 */
	protected ConditionError error(String message, Throwable cause, Map<String, Object> map) {
		logFailure(message, ex(cause, map));
		return new ConditionError(testId, getMessage() + ": " + message, cause);
	}

	/**
	 * Log a failure then return a ConditionError
	 */
	protected ConditionError error(String message, Map<String, Object> map) {
		logFailure(message, map);
		return new ConditionError(testId, getMessage() + ": " + message);
	}

	/**
	 * Log a failure then return a ConditionError
	 */
	protected ConditionError error(Throwable cause, Map<String, Object> map) {
		logFailure(cause.getMessage(), ex(cause, map));
		return new ConditionError(testId, getMessage(), cause);
	}

	/**
	 * Log a failure then return a ConditionError
	 */
	protected ConditionError error(String message, Throwable cause, JsonObject in) {
		logFailure(message, ex(cause, in));
		return new ConditionError(testId, getMessage() + ": " + message, cause);
	}

	/**
	 * Log a failure then return a ConditionError
	 */
	protected ConditionError error(String message, JsonObject in) {
		logFailure(message, in);
		return new ConditionError(testId, getMessage() + ": " + message);
	}

	/**
	 * Log a failure then return a ConditionError
	 */
	protected ConditionError error(Throwable cause, JsonObject in) {
		logFailure(cause.getMessage(), ex(cause, in));
		return new ConditionError(testId, getMessage(), cause);
	}

	/**
	 * Get the list of requirements that this test would fulfill if it passed
	 *
	 * @return
	 */
	protected Set<String> getRequirements() {
		return requirements;
	}

	protected String createBrowserInteractionPlaceholder(String msg) {
		String placeholder = RandomStringUtils.randomAlphanumeric(10);
		if (getRequirements().isEmpty()) {
			log(msg, args("upload", placeholder, "result", ConditionResult.REVIEW));
		} else {
			log(msg, args("upload", placeholder, "result", ConditionResult.REVIEW, "requirements", getRequirements()));
		}
		return placeholder;
	}

	protected String createBrowserInteractionPlaceholder() {
		String placeholder = RandomStringUtils.randomAlphanumeric(10);
		if (getRequirements().isEmpty()) {
			log(args("upload", placeholder, "result", ConditionResult.REVIEW));
		} else {
			log(args("upload", placeholder, "result", ConditionResult.REVIEW, "requirements", getRequirements()));
		}
		return placeholder;
	}

	/*
	 * Create an HTTP Client for use in calling outbound to other services
	 */
	protected HttpClient createHttpClient(Environment env) throws CertificateException, InvalidKeySpecException, NoSuchAlgorithmException, KeyStoreException, IOException, UnrecoverableKeyException, KeyManagementException {
		HttpClientBuilder builder = HttpClientBuilder.create()
			.useSystemProperties();

		KeyManager[] km = null;

		// initialize MTLS if it's available
		if (env.containsObject("mutual_tls_authentication")) {

			// TODO: move this to an extractor?
			String clientCert = env.getString("mutual_tls_authentication", "cert");
			String clientKey = env.getString("mutual_tls_authentication", "key");
			String clientCa = env.getString("mutual_tls_authentication", "ca");

			byte[] certBytes = Base64.getDecoder().decode(clientCert);
			byte[] keyBytes = Base64.getDecoder().decode(clientKey);

			X509Certificate cert = generateCertificateFromDER(certBytes);
			RSAPrivateKey key = generatePrivateKeyFromDER(keyBytes);

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

		builder.setSSLContext(sc);

		SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(sc,
			new String[] { "TLSv1.2" },
			null,
			NoopHostnameVerifier.INSTANCE);

		builder.setSSLSocketFactory(sslConnectionSocketFactory);

		Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory> create()
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

		restTemplate.getInterceptors().add(new LoggingRequestInterceptor(getMessage(), log, env.getObject("mutual_tls_authentication")));

		return restTemplate;
	}

	/**
	 * @param targetHost The host that will be used to create the socket.
	 * @param targetPort The port that will be used to create the socket.
	 * @return a newly created socket using the system HTTP proxy if one is set.
	 * @throws IOException thrown if there is an issue with the socket connection.
	 */
	protected Socket setupSocket(String targetHost, Integer targetPort) throws IOException {
		String proxyHost = System.getProperty("https.proxyHost", "");
		int proxyPort = Integer.parseInt(System.getProperty("https.proxyPort", "0"));
		Socket socket;
		if (!Strings.isNullOrEmpty(proxyHost) && proxyPort != 0) {

			// see https://gitlab.com/openid/conformance-suite/merge_requests/218#note_74098367
			log("Creating socket through system HTTPS proxy; this may cause incorrect test results", args(
					"proxy_host", proxyHost,
					"proxy_port", proxyPort,
					"target_host", targetHost,
					"target_port", targetPort,
					"result", ConditionResult.WARNING
				));
			// Note that the above 'log' doesn't make the test result be a warning; it would be better if it did but
			// that's not simple to achieve from here

			Proxy proxy = new Proxy(Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
			socket = new Socket(proxy);
			socket.connect(new InetSocketAddress(targetHost, targetPort));
		} else {
			socket = new Socket(InetAddress.getByName(targetHost), targetPort);
		}
		return socket;
	}

	protected static RSAPrivateKey generatePrivateKeyFromDER(byte[] keyBytes) throws InvalidKeySpecException, NoSuchAlgorithmException {
		PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);

		KeyFactory factory = KeyFactory.getInstance("RSA");

		return (RSAPrivateKey) factory.generatePrivate(spec);
	}

	protected static X509Certificate generateCertificateFromDER(byte[] certBytes) throws CertificateException {
		CertificateFactory factory = CertificateFactory.getInstance("X.509");

		return (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(certBytes));
	}

	protected static List<X509Certificate> generateCertificateChainFromDER(byte[] chainBytes) throws CertificateException {
		CertificateFactory factory = CertificateFactory.getInstance("X.509");

		ArrayList<X509Certificate> chain = new ArrayList<>();
		ByteArrayInputStream in = new ByteArrayInputStream(chainBytes);
		while (in.available() > 0) {
			chain.add((X509Certificate) factory.generateCertificate(in));
		}

		return chain;
	}

}
