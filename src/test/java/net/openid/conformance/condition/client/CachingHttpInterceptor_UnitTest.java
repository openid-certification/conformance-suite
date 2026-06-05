package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.logging.CachedHttpResponseMarker;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the interceptor-level behaviour that {@link ExternalEndpointCache_UnitTest}
 * does not: the opt-in eligibility gate, the GET-only restriction, the cache-key
 * partitioning by request header, the 2xx-only caching predicate, and the
 * hit-vs-populating-fetch distinction surfaced via {@link CachedHttpResponseMarker}.
 */
class CachingHttpInterceptor_UnitTest {

	@BeforeEach
	void clearCache() {
		// the interceptor uses the process-wide static cache
		ExternalEndpointCache.clear();
	}

	/** Counts how many times the real (downstream) execution runs and returns a
	 *  fresh response each time (the interceptor drains and closes the body). */
	private static final class CountingExecution implements ClientHttpRequestExecution {
		int calls = 0;
		int status = 200;
		String statusText = "OK";
		String body = "{\"ok\":true}";

		@Override
		public ClientHttpResponse execute(HttpRequest request, byte[] reqBody) {
			calls++;
			return new TestClientHttpResponse(status, statusText, body.getBytes(StandardCharsets.UTF_8));
		}
	}

	private static final class TestClientHttpResponse implements ClientHttpResponse {
		private final int status;
		private final String statusText;
		private final byte[] body;

		TestClientHttpResponse(int status, String statusText, byte[] body) {
			this.status = status;
			this.statusText = statusText;
			this.body = body;
		}

		@Override public HttpStatusCode getStatusCode() { return HttpStatusCode.valueOf(status); }
		@Override public String getStatusText() { return statusText; }
		@Override public HttpHeaders getHeaders() { return new HttpHeaders(); }
		@Override public InputStream getBody() { return new ByteArrayInputStream(body); }
		@Override public void close() { }
	}

	private static HttpRequest request(HttpMethod method, String url, String accept) {
		HttpHeaders headers = new HttpHeaders();
		if (accept != null) {
			headers.add("Accept", accept);
		}
		return new HttpRequest() {
			@Override public HttpMethod getMethod() { return method; }
			@Override public URI getURI() { return URI.create(url); }
			@Override public HttpHeaders getHeaders() { return headers; }
			@Override public java.util.Map<String, Object> getAttributes() { return new java.util.HashMap<>(); }
		};
	}

	private static Environment envWithCaching(boolean enabled) {
		Environment env = new Environment();
		JsonObject options = new JsonObject();
		options.addProperty("cache_external_metadata", enabled);
		JsonObject config = new JsonObject();
		config.add("options", options);
		env.putObject("config", config);
		return env;
	}

	private static String bodyOf(ClientHttpResponse resp) throws IOException {
		return StreamUtils.copyToString(resp.getBody(), StandardCharsets.UTF_8);
	}

	@Test
	void cachingDisabledByDefault_alwaysExecutes() throws IOException {
		CachingHttpInterceptor interceptor = new CachingHttpInterceptor(new Environment());
		CountingExecution exec = new CountingExecution();

		ClientHttpResponse r1 = interceptor.intercept(request(HttpMethod.GET, "https://op/jwks", null), new byte[0], exec);
		ClientHttpResponse r2 = interceptor.intercept(request(HttpMethod.GET, "https://op/jwks", null), new byte[0], exec);

		assertEquals(2, exec.calls, "with no opt-in, every request hits the network");
		assertFalse(r1 instanceof CachedHttpResponseMarker);
		assertFalse(r2 instanceof CachedHttpResponseMarker);
	}

	@Test
	void cachingDisabledWhenFlagFalse_alwaysExecutes() throws IOException {
		CachingHttpInterceptor interceptor = new CachingHttpInterceptor(envWithCaching(false));
		CountingExecution exec = new CountingExecution();

		interceptor.intercept(request(HttpMethod.GET, "https://op/jwks", null), new byte[0], exec);
		interceptor.intercept(request(HttpMethod.GET, "https://op/jwks", null), new byte[0], exec);

		assertEquals(2, exec.calls);
	}

	@Test
	void secondIdenticalGetIsServedFromCacheAndMarked() throws IOException {
		CachingHttpInterceptor interceptor = new CachingHttpInterceptor(envWithCaching(true));
		CountingExecution exec = new CountingExecution();

		ClientHttpResponse first = interceptor.intercept(request(HttpMethod.GET, "https://op/jwks", null), new byte[0], exec);
		ClientHttpResponse second = interceptor.intercept(request(HttpMethod.GET, "https://op/jwks", null), new byte[0], exec);

		assertEquals(1, exec.calls, "second identical GET must be a cache hit");
		// the populating fetch is NOT a cache hit ...
		assertFalse(first instanceof CachedHttpResponseMarker, "populating fetch must not be marked as cached");
		// ... but the replay is, so the log entry can be relabelled
		CachedHttpResponseMarker marker = assertInstanceOf(CachedHttpResponseMarker.class, second);
		assertTrue(marker.getCacheAgeSeconds() >= 0);
		// the body/status are replayed faithfully
		assertEquals("{\"ok\":true}", bodyOf(second));
		assertEquals(HttpStatusCode.valueOf(200), second.getStatusCode());
	}

	@Test
	void nonGetRequestIsNeverCached() throws IOException {
		CachingHttpInterceptor interceptor = new CachingHttpInterceptor(envWithCaching(true));
		CountingExecution exec = new CountingExecution();

		ClientHttpResponse r1 = interceptor.intercept(request(HttpMethod.POST, "https://op/par", null), new byte[]{1}, exec);
		ClientHttpResponse r2 = interceptor.intercept(request(HttpMethod.POST, "https://op/par", null), new byte[]{2}, exec);

		assertEquals(2, exec.calls, "POST must bypass the cache (key omits the body)");
		assertFalse(r1 instanceof CachedHttpResponseMarker);
		assertFalse(r2 instanceof CachedHttpResponseMarker);
	}

	@Test
	void differentAcceptHeadersAreCachedIndependently() throws IOException {
		CachingHttpInterceptor interceptor = new CachingHttpInterceptor(envWithCaching(true));
		CountingExecution exec = new CountingExecution();

		interceptor.intercept(request(HttpMethod.GET, "https://op/meta", "application/json"), new byte[0], exec);
		interceptor.intercept(request(HttpMethod.GET, "https://op/meta", "application/jwt"), new byte[0], exec);

		assertEquals(2, exec.calls, "Accept is part of the cache key, so the two requests must not collide");
	}

	@Test
	void non2xxResponseIsNotCached() throws IOException {
		CachingHttpInterceptor interceptor = new CachingHttpInterceptor(envWithCaching(true));
		CountingExecution exec = new CountingExecution();
		exec.status = 503;
		exec.statusText = "Service Unavailable";

		ClientHttpResponse first = interceptor.intercept(request(HttpMethod.GET, "https://op/jwks", null), new byte[0], exec);
		ClientHttpResponse second = interceptor.intercept(request(HttpMethod.GET, "https://op/jwks", null), new byte[0], exec);

		assertEquals(2, exec.calls, "a 503 must not be pinned in the cache");
		assertEquals(HttpStatusCode.valueOf(503), first.getStatusCode());
		assertFalse(second instanceof CachedHttpResponseMarker, "the re-fetch is a real network response, not a cache hit");
	}
}
