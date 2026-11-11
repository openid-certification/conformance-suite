package net.openid.conformance.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * IdpJwtDecoder's handling of the IdP being unreachable, and of what it tells the
 * caller. The checks applied to a token the IdP did sign are covered against real
 * signed tokens in IdpJwtDecoderChain_UnitTest.
 */
public class IdpJwtDecoder_UnitTest {

	private static final String ISSUER = "https://idp.example.com";

	private static final Duration RETRY = Duration.ofSeconds(30);

	private final AtomicLong now = new AtomicLong(1_000_000_000L);

	private static String tokenFrom(String issuer) throws JOSEException {
		SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.ES256),
			new JWTClaimsSet.Builder().issuer(issuer).subject("a-user").build());
		jwt.sign(new ECDSASigner(new ECKeyGenerator(Curve.P_256).generate()));
		return jwt.serialize();
	}

	private static Jwt aJwt() {
		return new Jwt("token", Instant.now(), Instant.now().plusSeconds(60),
			Map.of("typ", "at+jwt"), Map.of("sub", "a-user"));
	}

	private static JwtDecoder decoderReturning(Jwt jwt) {
		JwtDecoder delegate = Mockito.mock(JwtDecoder.class);
		Mockito.when(delegate.decode(Mockito.anyString())).thenReturn(jwt);
		return delegate;
	}

	private static JwtDecoder decoderThrowing(RuntimeException e) {
		JwtDecoder delegate = Mockito.mock(JwtDecoder.class);
		Mockito.when(delegate.decode(Mockito.anyString())).thenThrow(e);
		return delegate;
	}

	private IdpJwtDecoder decoder(Supplier<JwtDecoder> factory) {
		return new IdpJwtDecoder(ISSUER, factory, now::get, RETRY);
	}

	private static Supplier<JwtDecoder> counting(AtomicInteger builds, Supplier<JwtDecoder> factory) {
		return () -> {
			builds.incrementAndGet();
			return factory.get();
		};
	}

	@Test
	public void the_delegate_is_not_built_until_a_token_is_decoded() throws Exception {
		AtomicInteger builds = new AtomicInteger();
		IdpJwtDecoder decoder = decoder(counting(builds, () -> decoderReturning(aJwt())));

		Assertions.assertEquals(0, builds.get(),
			"constructing the decoder must not reach the IdP - that would tie startup to it");

		decoder.decode(tokenFrom(ISSUER));
		Assertions.assertEquals(1, builds.get());
	}

	@Test
	public void the_delegate_is_built_once_across_repeated_decodes() throws Exception {
		AtomicInteger builds = new AtomicInteger();
		JwtDecoder delegate = decoderReturning(aJwt());
		IdpJwtDecoder decoder = decoder(counting(builds, () -> delegate));

		String token = tokenFrom(ISSUER);
		decoder.decode(token);
		decoder.decode(token);
		decoder.decode(token);

		Assertions.assertEquals(1, builds.get(), "the discovery document must not be re-fetched per request");
		Mockito.verify(delegate, Mockito.times(3)).decode(token);
	}

	/**
	 * Any bearer token the other providers do not recognise reaches this decoder,
	 * so one that does not even claim to be from the IdP must not cause a request
	 * to it.
	 */
	@Test
	public void a_token_that_is_not_from_the_issuer_never_reaches_the_idp() throws Exception {
		AtomicInteger builds = new AtomicInteger();
		IdpJwtDecoder decoder = decoder(counting(builds, () -> decoderReturning(aJwt())));

		for (String token : List.of(
			tokenFrom("https://somewhere-else.example.com"),
			new PlainJWT(new JWTClaimsSet.Builder().issuer(ISSUER).build()).serialize(),
			"an-opaque-api-token",
			"a.b.c")) {
			BadJwtException e = Assertions.assertThrows(BadJwtException.class, () -> decoder.decode(token), token);
			Assertions.assertEquals(IdpJwtDecoder.REJECTED, e.getMessage());
		}

		Assertions.assertEquals(0, builds.get());
	}

	/**
	 * The failure must come out as BadJwtException, the one type that
	 * JwtAuthenticationProvider turns into a 401; anything else escapes the filter
	 * chain as a server error.
	 */
	@Test
	public void a_discovery_failure_is_a_401_and_is_not_retried_until_the_backoff_expires() throws Exception {
		AtomicInteger builds = new AtomicInteger();
		JwtDecoder working = decoderReturning(aJwt());
		IdpJwtDecoder decoder = decoder(counting(builds, () -> {
			if (builds.get() == 1) {
				throw new IllegalArgumentException("Unable to resolve the Configuration with the provided Issuer");
			}
			return working;
		}));
		String token = tokenFrom(ISSUER);

		BadJwtException first = Assertions.assertThrows(BadJwtException.class, () -> decoder.decode(token));
		Assertions.assertEquals(IdpJwtDecoder.REJECTED, first.getMessage());
		Assertions.assertNotNull(first.getCause(), "the underlying cause must be preserved for the logs");

		now.addAndGet(RETRY.minusSeconds(1).toNanos());
		Assertions.assertThrows(BadJwtException.class, () -> decoder.decode(token));
		Assertions.assertEquals(1, builds.get(), "a request inside the backoff window must not reach the IdP");

		now.addAndGet(Duration.ofSeconds(1).toNanos());
		Assertions.assertEquals("a-user", decoder.decode(token).getSubject(),
			"the retry after the backoff must succeed once the IdP is back");
		Assertions.assertEquals(2, builds.get());
	}

	/**
	 * Once discovery has succeeded, a failure to fetch the signing keys arrives from
	 * NimbusJwtDecoder as a plain JwtException, which JwtAuthenticationProvider
	 * would otherwise turn into a server error.
	 */
	@Test
	public void a_jwks_fetch_failure_is_a_401() throws Exception {
		IdpJwtDecoder decoder = decoder(() -> decoderThrowing(new JwtException(
			"An error occurred while attempting to decode the Jwt: Couldn't retrieve remote JWK set: Read timed out")));

		BadJwtException e = Assertions.assertThrows(BadJwtException.class, () -> decoder.decode(tokenFrom(ISSUER)));

		Assertions.assertEquals(IdpJwtDecoder.REJECTED, e.getMessage());
		Assertions.assertNotNull(e.getCause(), "the underlying cause must be preserved for the logs");
	}

	/**
	 * The response goes to anonymous callers, so it must not say why a token was
	 * refused (for example that it was real but has expired).
	 */
	@Test
	public void a_validator_rejection_does_not_reach_the_caller() throws Exception {
		JwtValidationException validationFailure = new JwtValidationException("Jwt expired at 2026-01-01T00:00:00Z",
			List.of(new OAuth2Error("invalid_token", "Jwt expired at 2026-01-01T00:00:00Z", null)));
		IdpJwtDecoder decoder = decoder(() -> decoderThrowing(validationFailure));

		BadJwtException e = Assertions.assertThrows(BadJwtException.class, () -> decoder.decode(tokenFrom(ISSUER)));

		Assertions.assertEquals(IdpJwtDecoder.REJECTED, e.getMessage());
		Assertions.assertSame(validationFailure, e.getCause());
	}

	@Test
	public void a_successful_decode_is_passed_through_unchanged() throws Exception {
		Jwt jwt = aJwt();
		IdpJwtDecoder decoder = decoder(() -> decoderReturning(jwt));

		Assertions.assertSame(jwt, decoder.decode(tokenFrom(ISSUER)));
	}
}
