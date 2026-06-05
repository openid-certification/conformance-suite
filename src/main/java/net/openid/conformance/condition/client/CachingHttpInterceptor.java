package net.openid.conformance.condition.client;

import net.openid.conformance.logging.CachedHttpResponseMarker;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Optional;

/**
 * Spring {@link ClientHttpRequestInterceptor} that consults
 * {@link ExternalEndpointCache} before issuing the real HTTP call.
 * Opt-in: install via {@code AbstractCondition.createRestTemplateWithCache}.
 *
 * <p>Cache key = {@code METHOD " " URI " | Accept=" h " | Accept-Language=" h}
 * — request headers that change the response (Accept for signed-vs-unsigned
 * metadata, Accept-Language for content-negotiation) are part of the key, so
 * two tests issuing different headers don't collide.
 *
 * <p>Eligibility gate (see {@link #shouldCache}): a test config opts in by
 * setting {@code "options": {"cache_external_metadata": true}}. Default off.
 * Only {@code GET} requests are cached (metadata/JWKS fetches); any other
 * method is passed straight through, since the cache key does not incorporate
 * a request body.
 *
 * <p><b>Do not enable this for tests that deliberately observe a metadata or
 * JWKS change at the same URL</b> (e.g. {@code oidcc-server-rotate-keys},
 * which re-fetches {@code jwks_uri} after a key rotation and expects to see
 * the new key). Within the {@link ExternalEndpointCache} TTL the second fetch
 * would be served the stale pre-rotation response. The opt-in CI configs that
 * set the flag today do not exercise any such rotation flow.
 *
 * <p>On a cache hit a synthetic {@link ClientHttpResponse} is returned
 * (tagged with {@link CachedHttpResponseMarker}) without invoking the
 * downstream execution, so the real HTTP call is skipped. The outer
 * {@link net.openid.conformance.logging.LoggingRequestInterceptor} detects
 * the marker and relabels its single "HTTP response" log entry as
 * "Using cached HTTP response", so the test event log makes the cache hit
 * visible without a duplicate entry.
 */
public class CachingHttpInterceptor implements ClientHttpRequestInterceptor {

	private final Environment env;

	public CachingHttpInterceptor(Environment env) {
		this.env = env;
	}

	@Override
	public ClientHttpResponse intercept(HttpRequest req, byte[] body, ClientHttpRequestExecution exec) throws IOException {
		// Only GET is cached: the cache key omits the request body, so caching
		// any body-bearing method could serve one request's response to another.
		if (!shouldCache() || !HttpMethod.GET.equals(req.getMethod())) {
			return exec.execute(req, body);
		}

		String key = buildKey(req);
		Optional<ExternalEndpointCache.Entry> probe = ExternalEndpointCache.get(key);
		if (probe.isPresent()) {
			// LoggingRequestInterceptor (registered outside us) will detect the
			// CachedHttpResponseMarker and relabel its single "HTTP response"
			// log entry as "Using cached HTTP response" — no separate entry
			// from here.
			return new CachedClientHttpResponse(probe.get());
		}

		try {
			// Cache 2xx only - transient 4xx/5xx must not be pinned for the
			// TTL window. Single-flight still applies on a miss so concurrent
			// failed fetches share the same response without each issuing
			// its own HTTP call.
			ExternalEndpointCache.Entry fresh = ExternalEndpointCache.getOrFetch(key,
				() -> capture(exec.execute(req, body)),
				e -> e.statusCode() >= 200 && e.statusCode() < 300);
			// Miss path: a network fetch actually happened (either by us or
			// the single-flight winner). Return the un-marked variant so
			// LoggingRequestInterceptor logs it as the normal "HTTP response",
			// not a cache hit — cache "hits" must reflect reuse of a
			// previously-populated entry, not the populating fetch itself.
			return new ReplayedClientHttpResponse(fresh);
		} catch (IOException | RuntimeException e) {
			throw e;
		} catch (Exception e) {
			// Fetcher only throws IOException via exec.execute(), so this is unreachable.
			throw new IOException("cache fetch failed for " + key, e);
		}
	}

	/** A test config opts into caching by setting
	 *  {@code "options": {"cache_external_metadata": true}}. */
	private boolean shouldCache() {
		return Boolean.TRUE.equals(env.getBoolean("config", "options.cache_external_metadata"));
	}

	/** Build a cache key that incorporates the request method, URI, and the
	 *  headers that can change the response (Accept governs JSON vs JWT for
	 *  signed metadata; Accept-Language affects localized metadata). */
	private static String buildKey(HttpRequest req) {
		HttpHeaders headers = req.getHeaders();
		return req.getMethod().name() + " " + req.getURI()
			+ " | Accept=" + headers.getFirst("Accept")
			+ " | Accept-Language=" + headers.getFirst("Accept-Language");
	}

	/** Drain the response body into a byte array and snapshot the status +
	 *  headers, so the original (streaming) response can be discarded and
	 *  replayed from the cache. */
	private static ExternalEndpointCache.Entry capture(ClientHttpResponse resp) throws IOException {
		byte[] bodyBytes;
		try (InputStream in = resp.getBody()) {
			bodyBytes = StreamUtils.copyToByteArray(in);
		} finally {
			resp.close();
		}
		HttpHeaders snapshot = new HttpHeaders();
		snapshot.putAll(resp.getHeaders());
		return new ExternalEndpointCache.Entry(
			bodyBytes,
			resp.getStatusCode().value(),
			resp.getStatusText(),
			HttpHeaders.readOnlyHttpHeaders(snapshot),
			Instant.now());
	}

	/** Synthesizes a {@link ClientHttpResponse} from a cache entry, used on
	 *  the miss path so the captured body can be returned upstream after the
	 *  fetcher has drained and closed the real streaming response. Does NOT
	 *  carry the {@link CachedHttpResponseMarker}, so
	 *  {@link net.openid.conformance.logging.LoggingRequestInterceptor} logs
	 *  it as a normal "HTTP response". */
	private static class ReplayedClientHttpResponse implements ClientHttpResponse {
		protected final ExternalEndpointCache.Entry entry;

		ReplayedClientHttpResponse(ExternalEndpointCache.Entry entry) {
			this.entry = entry;
		}

		@Override
		public HttpStatusCode getStatusCode() {
			return HttpStatusCode.valueOf(entry.statusCode());
		}

		@Override
		public String getStatusText() {
			return entry.statusText();
		}

		@Override
		public HttpHeaders getHeaders() {
			return entry.headers();
		}

		@Override
		public InputStream getBody() {
			return new ByteArrayInputStream(entry.body());
		}

		@Override
		public void close() {
			// no underlying resource
		}
	}

	/** True cache-hit variant of {@link ReplayedClientHttpResponse}, returned
	 *  only when the entry was already present at lookup time. The
	 *  {@link CachedHttpResponseMarker} tells
	 *  {@link net.openid.conformance.logging.LoggingRequestInterceptor} to
	 *  relabel its log entry as "Using cached HTTP response". */
	private static final class CachedClientHttpResponse extends ReplayedClientHttpResponse implements CachedHttpResponseMarker {
		CachedClientHttpResponse(ExternalEndpointCache.Entry entry) {
			super(entry);
		}

		@Override
		public long getCacheAgeSeconds() {
			return entry.ageSeconds();
		}
	}
}
