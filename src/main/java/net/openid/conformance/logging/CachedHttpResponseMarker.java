package net.openid.conformance.logging;

/**
 * Implemented by synthesized {@link org.springframework.http.client.ClientHttpResponse}
 * instances that replay a previously cached response, so
 * {@link LoggingRequestInterceptor} can label the resulting log entry as a
 * cache hit rather than emitting a separate one.
 */
// Not @FunctionalInterface: this is a marker mixed into ClientHttpResponse
// implementations, not a callback meant to be passed as a lambda.
@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface CachedHttpResponseMarker {
	long getCacheAgeSeconds();
}
