package net.openid.conformance.security;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoderInitializationException;
import org.springframework.security.oauth2.jwt.SupplierJwtDecoder;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * WebSecurityResourceServerConfig wraps the IdP's decoder in a
 * {@link SupplierJwtDecoder} so that resolving the IdP's discovery document — a
 * network call — happens on the first token rather than during bean creation.
 * Without that, the conformance suite cannot start at all while the IdP is
 * unreachable, including for the many tests that never authenticate.
 * <p>
 * These tests pin the three properties of SupplierJwtDecoder that the startup
 * guarantee rests on, so a Spring upgrade that changed any of them fails here
 * rather than silently reintroducing the coupling.
 */
public class IdpJwtDecoderLaziness_UnitTest {

	private static Jwt aJwt() {
		return new Jwt("token", Instant.now(), Instant.now().plusSeconds(60),
			Map.of("typ", "jwt"), Map.of("sub", "a-user"));
	}

	private static JwtDecoder decoderReturning(Jwt jwt) {
		JwtDecoder delegate = Mockito.mock(JwtDecoder.class);
		Mockito.when(delegate.decode(Mockito.anyString())).thenReturn(jwt);
		return delegate;
	}

	@Test
	public void the_delegate_is_not_built_until_a_token_is_decoded() {
		AtomicInteger builds = new AtomicInteger();

		SupplierJwtDecoder decoder = new SupplierJwtDecoder(() -> {
			builds.incrementAndGet();
			return decoderReturning(aJwt());
		});

		Assertions.assertEquals(0, builds.get(),
			"constructing the decoder must not reach the IdP - that would tie startup to it");

		decoder.decode("a-token");
		Assertions.assertEquals(1, builds.get());
	}

	@Test
	public void the_delegate_is_built_once_across_repeated_decodes() {
		AtomicInteger builds = new AtomicInteger();
		JwtDecoder delegate = decoderReturning(aJwt());

		SupplierJwtDecoder decoder = new SupplierJwtDecoder(() -> {
			builds.incrementAndGet();
			return delegate;
		});

		decoder.decode("one");
		decoder.decode("two");
		decoder.decode("three");

		Assertions.assertEquals(1, builds.get(), "the discovery document must not be re-fetched per request");
		Mockito.verify(delegate, Mockito.times(3)).decode(Mockito.anyString());
	}

	/**
	 * A decoder that remembered the failure would stay broken for the life of the
	 * process after a transient IdP outage, and only a restart would fix it.
	 * <p>
	 * Note the failure type: JwtDecoderInitializationException extends
	 * RuntimeException, NOT JwtException, so on its own it escapes the filter chain
	 * as a server error. IdpJwtDecoder exists to translate it — see
	 * IdpJwtDecoder_UnitTest.
	 */
	@Test
	public void a_failed_build_is_retried_on_the_next_request() {
		AtomicInteger builds = new AtomicInteger();

		SupplierJwtDecoder decoder = new SupplierJwtDecoder(() -> {
			if (builds.incrementAndGet() == 1) {
				throw new IllegalStateException("IdP unreachable");
			}
			return decoderReturning(aJwt());
		});

		Assertions.assertThrows(JwtDecoderInitializationException.class, () -> decoder.decode("a-token"));

		Jwt jwt = decoder.decode("a-token");
		Assertions.assertEquals("a-user", jwt.getSubject(), "the retry must succeed once the IdP is back");
		Assertions.assertEquals(2, builds.get());
	}
}
