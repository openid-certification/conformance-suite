package net.openid.conformance.condition.common.sockets;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.FunctionalCondition;
import net.openid.conformance.logging.HttpRequestDeadlineInterceptor;
import net.openid.conformance.testmodule.ConditionCallBuilder;
import net.openid.conformance.testmodule.DataUtils;
import net.openid.conformance.testmodule.Environment;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.TlsConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.TlsSocketStrategy;
import org.apache.hc.core5.http.ssl.TLS;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.util.Timeout;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public class Sockets {

	private static final DataUtils DATA_UTILS = new DataUtils() {
	};

	/**
	 * GET the resource and store the response body in {@code targetEnvName}.
	 *
	 * @param name identifies the call in the test log, in expected-failure lists and in {@code Steps.modify}; it must
	 *             be stable and unique within the test module
	 */
	public static ConditionCallBuilder getResource(
		String name,
		String targetEnvName,
		String resourceDescription,
		Function<Environment, URI> hostResolver,
		Function<Environment, HttpHeaders> headersResolver
	) {
		return resource(name, targetEnvName, resourceDescription,HttpMethod.GET, hostResolver, null, headersResolver, null);
	}

	public static ConditionCallBuilder getResource(
		String name,
		String targetEnvName,
		String resourceDescription,
		Function<Environment, URI> hostResolver,
		Function<Environment, HttpHeaders> headersResolver,
		Function<Environment, MtlsSettings> mtlsSettings
	) {
		return resource(name, targetEnvName, resourceDescription,HttpMethod.GET, hostResolver, null, headersResolver, mtlsSettings);
	}

	public static ConditionCallBuilder postResource(
		String name,
		String targetEnvName,
		String resourceDescription,
		Function<Environment, URI> hostResolver,
		Function<Environment, ?> bodyResolver,
		Function<Environment, HttpHeaders> headersResolver,
		Function<Environment, MtlsSettings> mtlsSettings
	) {
		return resource(name, targetEnvName, resourceDescription,HttpMethod.POST, hostResolver, bodyResolver, headersResolver, mtlsSettings);
	}

	public static ConditionCallBuilder deleteResource(
		String name,
		String targetEnvName,
		String resourceDescription,
		Function<Environment, URI> hostResolver,
		Function<Environment, HttpHeaders> headersResolver,
		Function<Environment, MtlsSettings> mtlsSettings
	) {
		return resource(name, targetEnvName, resourceDescription, HttpMethod.DELETE, hostResolver, null, headersResolver, mtlsSettings);
	}


	private static ConditionCallBuilder resource(
		String name,
		String targetEnvName,
		String resourceDescription,
		HttpMethod method,
		Function<Environment, URI> hostResolver,
		Function<Environment, ?> bodyResolver,
		Function<Environment, HttpHeaders> headersResolver,
		Function<Environment, MtlsSettings> mtlsSettings
	) {
		return new ConditionCallBuilder(new FunctionalCondition(name, (env, c) -> {
			var resourceEndpoint = hostResolver.apply(env);
			MtlsSettings mtls = mtlsSettings == null ? null : mtlsSettings.apply(env);
			try (CloseableHttpClient httpClient = createHttpClient(() -> mtls)) {

				RestTemplate restTemplate = createRestTemplate(httpClient,
					c.loggingRequestInterceptor(env, mtls == null ? null : mtlsForLog(mtls)));

				HttpHeaders headers = new HttpHeaders();

				if (headersResolver != null) {
					headers = headersResolver.apply(env);
				}

				HttpEntity<?> request;
				if (bodyResolver != null) {
					request = new HttpEntity<>(bodyResolver.apply(env), headers);
				} else {
					request = new HttpEntity<>(null, headers);
				}

				ResponseEntity<String> response = restTemplate.exchange(resourceEndpoint, method, request, String.class);

				JsonObject resp = convertResponseForEnvironment(name, response);
				env.putObject(targetEnvName, resp);
				env.putObject("last_http_call", resp);
				env.putString("last_http_call_description", resourceDescription);
				c.log("Stored the response from the endpoint", Map.of(
					"endpoint_name", name,
					"status", response.getStatusCode().value(),
					"stored_in", targetEnvName));
			} catch (NoSuchAlgorithmException | KeyManagementException | CertificateException |
				InvalidKeySpecException | KeyStoreException | IOException | UnrecoverableKeyException e) {
				throw c.error("Error creating HTTP Client", e);
			} catch (RestClientException e) {
				String msg = "Call to endpoint " + resourceEndpoint + " failed";
				if (e.getCause() != null) {
					msg += " - " + e.getCause().getMessage();
				}
				throw c.error(msg, e);
			}
		}));
	}


	static CloseableHttpClient createHttpClient(Supplier<MtlsSettings> mtlsSettingsSupplier)
		throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {

		// Trust standard CA and those trusted by our custom strategy
		var sslContectFactory = SSLContexts.custom()
			// TrustStrategy To trust any certificate.
			.loadTrustMaterial((chain, authType) -> true);

		// initialize MTLS if it's available
		if (mtlsSettingsSupplier != null) {

			try {
				KeyStore keyStore = createInMemoryKeyStore(mtlsSettingsSupplier, "clientCert");
				if (keyStore != null) {
					sslContectFactory.loadKeyMaterial(keyStore, new char[0]);
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		final SSLContext sslContext = sslContectFactory.build();

		final TlsSocketStrategy tlsStrategy = new DefaultClientTlsStrategy(sslContext);

		final HttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder.create()
			.setTlsSocketStrategy(tlsStrategy)
			.setDefaultTlsConfig(TlsConfig.custom()
				.setHandshakeTimeout(Timeout.ofSeconds(30))
				.setSupportedProtocols(TLS.V_1_3)
				.build())
			.build();

		return HttpClients.custom()
			.setConnectionManager(cm)
			.build();

	}

	static RestTemplate createRestTemplate(HttpClient httpClient, ClientHttpRequestInterceptor loggingInterceptor) throws UnrecoverableKeyException, KeyManagementException, CertificateException, InvalidKeySpecException, NoSuchAlgorithmException, KeyStoreException, IOException {

		RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));

		// Hard wall-clock deadline on the whole request (including response-body buffering). The
		// socket/response timeouts above are per-read only, so a trickling peer can defeat them; this
		// aborts the request by closing the client if the call exceeds the timeout. Added first so it is
		// the outermost interceptor and therefore also covers the body buffering done by the logging
		// interceptor. See https://gitlab.com/openid/conformance-suite/-/work_items/1827
		restTemplate.getInterceptors().add(new HttpRequestDeadlineInterceptor(
			() -> ((CloseableHttpClient) httpClient).close(CloseMode.IMMEDIATE), 60));

		// Single interceptor handles both logging and lock release. The lock is released only
		// during network I/O (HTTP call + response body buffering); logging happens with the
		// lock held for deterministic ordering relative to other threads.
		restTemplate.getInterceptors().add(loggingInterceptor);

		// Every HTTP status is a response to store, not an error, so the conditions that run afterwards can check it
		restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
			@Override
			public boolean hasError(ClientHttpResponse response) {
				return false;
			}
		});

		List<HttpMessageConverter<?>> converters = restTemplate.getMessageConverters();

		// fix the StringHttpMessageConverter, but retaining other default converters, as we do use them,
		// e.g. the map -> urlencoded-form body one
		converters.stream()
			.filter(converter -> converter instanceof StringHttpMessageConverter)
			.forEach(converter -> {
				StringHttpMessageConverter stringHttpMessageConverter = (StringHttpMessageConverter) converter;
				// the default StringHttpMessageConverter will convert to Latin1, so override it
				stringHttpMessageConverter.setDefaultCharset(StandardCharsets.UTF_8);
				// Stop StringHttpMessageConverter from adding a default Accept-Charset header
				stringHttpMessageConverter.setWriteAcceptCharset(false);
			});

		return restTemplate;
	}


	/**
	 * The response in the shape the standard endpoint call conditions store: status, endpoint_name, headers (lower
	 * case names, an array for a header sent more than once), body, and body_json when the body is a JSON object or
	 * array and the Content-Type is absent or JSON-compatible.
	 */
	static JsonObject convertResponseForEnvironment(String endpointName, ResponseEntity<String> response) {
		JsonObject resp = new JsonObject();
		resp.addProperty("status", response.getStatusCode().value());
		resp.addProperty("endpoint_name", endpointName);
		resp.add("headers", DATA_UTILS.mapToJsonObject(response.getHeaders(), true));
		String body = response.getBody();
		resp.addProperty("body", body);

		MediaType contentType = response.getHeaders().getContentType();
		if (body != null && (contentType == null || MediaType.APPLICATION_JSON.isCompatibleWith(contentType))) {
			try {
				JsonElement jsonRoot = JsonParser.parseString(body);
				if (jsonRoot.isJsonObject() || jsonRoot.isJsonArray()) {
					resp.add("body_json", jsonRoot);
				}
			} catch (JsonParseException e) {
				// not JSON after all: body still holds the raw response
			}
		}
		return resp;
	}

	/**
	 * The mTLS settings in the shape of the "mutual_tls_authentication" environment object, for the HTTP request log.
	 */
	static JsonObject mtlsForLog(MtlsSettings mtls) {
		JsonObject o = new JsonObject();
		try {
			o.addProperty("cert", toPem("CERTIFICATE", mtls.clientCert()[0].getEncoded()));
			if (mtls.clientCert().length > 1) {
				StringBuilder ca = new StringBuilder();
				for (int i = 1; i < mtls.clientCert().length; i++) {
					ca.append(toPem("CERTIFICATE", mtls.clientCert()[i].getEncoded()));
				}
				o.addProperty("ca", ca.toString());
			}
		} catch (CertificateEncodingException e) {
			throw new IllegalStateException("Unable to encode the mTLS client certificate", e);
		}
		o.addProperty("key", toPem("PRIVATE KEY", mtls.clientKey().getEncoded()));
		return o;
	}

	private static String toPem(String type, byte[] der) {
		return "-----BEGIN " + type + "-----\n"
			+ Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII)).encodeToString(der)
			+ "\n-----END " + type + "-----\n";
	}

	public static KeyStore createInMemoryKeyStore(
		Supplier<MtlsSettings> mtlsSettings,
		String alias,
		char... keyPassword) throws Exception {

		MtlsSettings mtls = mtlsSettings.get();
		if (mtls == null) {
			return null;
		}

		// 1. Initialize an empty in-memory KeyStore (PKCS12 is the modern default)
		KeyStore keyStore = KeyStore.getInstance("PKCS12");
		keyStore.load(null, null);


		keyStore.setKeyEntry(alias, mtls.clientKey(), keyPassword, mtls.clientCert());

		return keyStore;
	}


}
