package net.openid.conformance.security;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoderInitializationException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.core.OAuth2Error;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * The IdP's decoder is resolved lazily, so a request can be the first thing to
 * discover that the IdP is unreachable. That arrives as one of two types, neither
 * of which produces a 401 on its own: JwtDecoderInitializationException from
 * discovery, which extends RuntimeException rather than JwtException so nothing in
 * the filter chain recognises it as an authentication failure; and a plain
 * JwtException from the JWKS fetch, which JwtAuthenticationProvider turns into an
 * AuthenticationServiceException that AuthenticationEntryPointFailureHandler
 * re-throws. Both escape as server errors. These tests pin the translation that
 * keeps such a request answering 401.
 */
public class IdpJwtDecoder_UnitTest {

	private static final String TOKEN = "a-token";

	/**
	 * BadJwtException specifically, not a plain JwtException: the provider maps
	 * JwtException to AuthenticationServiceException, and
	 * AuthenticationEntryPointFailureHandler re-throws those by default rather than
	 * committing a 401 — which is the escaping-exception behaviour being fixed.
	 */
	@Test
	public void a_decoder_initialization_failure_becomes_a_bad_jwt_exception() {
		JwtDecoder failing = Mockito.mock(JwtDecoder.class);
		Mockito.when(failing.decode(TOKEN)).thenThrow(
			new JwtDecoderInitializationException("Failed to lazily resolve the supplied JwtDecoder instance",
				new IllegalArgumentException("Unable to resolve the Configuration with the provided Issuer")));

		BadJwtException e = Assertions.assertThrows(BadJwtException.class,
			() -> new IdpJwtDecoder(failing).decode(TOKEN));

		Assertions.assertFalse(JwtDecoderInitializationException.class.isInstance(e),
			"the untranslated type escapes BearerTokenAuthenticationFilter as a server error");
		Assertions.assertNotNull(e.getCause(), "the underlying cause must be preserved for the logs");
	}

	/**
	 * Discovery is memoized by SupplierJwtDecoder, so once it has succeeded the next
	 * way to fail is the JWKS fetch — NimbusJwtDecoder's RemoteKeySourceException and
	 * JOSEException branches, which throw a plain JwtException rather than a
	 * BadJwtException. Untranslated that is a 500, which is the same escaping
	 * behaviour this class exists to prevent, one step further along.
	 */
	@Test
	public void a_jwks_fetch_failure_becomes_a_bad_jwt_exception() {
		JwtDecoder failing = Mockito.mock(JwtDecoder.class);
		Mockito.when(failing.decode(TOKEN)).thenThrow(
			new JwtException("An error occurred while attempting to decode the Jwt: "
				+ "Couldn't retrieve remote JWK set: Read timed out"));

		BadJwtException e = Assertions.assertThrows(BadJwtException.class,
			() -> new IdpJwtDecoder(failing).decode(TOKEN));

		Assertions.assertNotNull(e.getCause(), "the underlying cause must be preserved for the logs");
	}

	/**
	 * JwtValidationException — expiry, issuer, audience, and so the
	 * IdpAudienceValidator's own rejections — is a BadJwtException subclass. Widening
	 * the translation to JwtException must not have swallowed those messages, which
	 * are what tells an operator which check actually failed.
	 */
	@Test
	public void a_validator_rejection_keeps_its_own_message() {
		OAuth2Error error = new OAuth2Error("invalid_token", "The aud claim is not valid", null);
		JwtValidationException validationFailure =
			new JwtValidationException("The token is not valid", List.of(error));
		JwtDecoder delegate = Mockito.mock(JwtDecoder.class);
		Mockito.when(delegate.decode(TOKEN)).thenThrow(validationFailure);

		JwtValidationException e = Assertions.assertThrows(JwtValidationException.class,
			() -> new IdpJwtDecoder(delegate).decode(TOKEN));

		Assertions.assertSame(validationFailure, e);
	}

	@Test
	public void a_successful_decode_is_passed_through_unchanged() {
		Jwt jwt = new Jwt(TOKEN, Instant.now(), Instant.now().plusSeconds(60),
			Map.of("typ", "jwt"), Map.of("sub", "a-user"));
		JwtDecoder delegate = Mockito.mock(JwtDecoder.class);
		Mockito.when(delegate.decode(TOKEN)).thenReturn(jwt);

		Assertions.assertSame(jwt, new IdpJwtDecoder(delegate).decode(TOKEN));
	}

	/**
	 * A genuinely bad token must keep its own failure type: JwtAuthenticationProvider
	 * maps BadJwtException to InvalidBearerTokenException, which carries the
	 * "invalid_token" WWW-Authenticate detail. Collapsing everything into one type
	 * would lose that.
	 */
	@Test
	public void an_invalid_token_failure_is_left_alone() {
		JwtDecoder delegate = Mockito.mock(JwtDecoder.class);
		Mockito.when(delegate.decode(TOKEN)).thenThrow(new BadJwtException("Malformed token"));

		BadJwtException e = Assertions.assertThrows(BadJwtException.class,
			() -> new IdpJwtDecoder(delegate).decode(TOKEN));

		Assertions.assertEquals("Malformed token", e.getMessage(),
			"a bad token's own message must survive; only unreachable-IdP failures are re-worded");
	}
}
