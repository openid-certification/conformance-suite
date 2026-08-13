package net.openid.conformance.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoderInitializationException;
import org.springframework.security.oauth2.jwt.JwtException;

/**
 * Decodes IdP-issued tokens, translating a failure to resolve the IdP's
 * configuration into an authentication failure.
 * <p>
 * The delegate resolves the IdP's discovery document on first use, so that
 * startup does not depend on the IdP being reachable. When that resolution
 * fails — the IdP is down, or a cache in front of it times out — Spring's
 * {@link JwtDecoderInitializationException} is thrown, and it extends
 * RuntimeException rather than {@link JwtException}. Nothing in the filter chain
 * treats it as an authentication failure: it escapes
 * {@code BearerTokenAuthenticationFilter}, which only catches
 * AuthenticationException, and surfaces as an unhandled server error instead of
 * a 401.
 * <p>
 * Re-thrown as a {@link BadJwtException}, which {@code JwtAuthenticationProvider}
 * maps to InvalidBearerTokenException and the filter answers with 401. A plain
 * JwtException would instead become an AuthenticationServiceException, and
 * {@code AuthenticationEntryPointFailureHandler} re-throws those by default
 * ({@code rethrowAuthenticationServiceException} is true) — straight back to the
 * escaping-exception behaviour this class exists to prevent.
 * <p>
 * This is fail-closed: a token that cannot be verified never authenticates
 * anyone. The trade-off is that the response says "invalid_token" when the real
 * problem is that the IdP is unreachable, so the true cause is logged at ERROR.
 */
public final class IdpJwtDecoder implements JwtDecoder {

	private static final Logger log = LoggerFactory.getLogger(IdpJwtDecoder.class);

	private final JwtDecoder delegate;

	public IdpJwtDecoder(JwtDecoder delegate) {
		this.delegate = delegate;
	}

	@Override
	public Jwt decode(String token) throws JwtException {
		try {
			return delegate.decode(token);
		} catch (JwtDecoderInitializationException e) {
			// The caller is told "invalid_token", which is not the real reason, so the
			// real one has to be visible somewhere.
			log.error("Unable to resolve the IdP's configuration; rejecting the bearer token as unverifiable", e);
			throw new BadJwtException(
				"Unable to verify the token: the IdP's configuration could not be resolved", e);
		}
	}
}
