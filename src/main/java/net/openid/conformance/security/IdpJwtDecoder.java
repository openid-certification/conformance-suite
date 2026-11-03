package net.openid.conformance.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoderInitializationException;
import org.springframework.security.oauth2.jwt.JwtException;

/**
 * Decodes IdP-issued tokens, translating a failure to reach the IdP into an
 * authentication failure rather than a server error.
 * <p>
 * The delegate resolves the IdP's discovery document on first use, so that
 * startup does not depend on the IdP being reachable. Reaching the IdP can then
 * fail on a request, in two distinct ways, and neither ends in a 401 on its own:
 * <ul>
 * <li><b>Discovery.</b> {@code SupplierJwtDecoder} throws Spring's
 * {@link JwtDecoderInitializationException}, which extends RuntimeException
 * rather than {@link JwtException}. Nothing in the filter chain treats it as an
 * authentication failure: it escapes {@code BearerTokenAuthenticationFilter},
 * which only catches AuthenticationException, and surfaces as an unhandled
 * server error.</li>
 * <li><b>JWKS.</b> Discovery is memoized, so once it has succeeded a later
 * failure to fetch the signing keys comes from {@code NimbusJwtDecoder} instead,
 * as a <em>plain</em> {@link JwtException} (its RemoteKeySourceException and
 * JOSEException branches). {@code JwtAuthenticationProvider} maps BadJwtException
 * to InvalidBearerTokenException — a 401 — but maps every other JwtException to
 * AuthenticationServiceException, and
 * {@code AuthenticationEntryPointFailureHandler} re-throws those by default
 * ({@code rethrowAuthenticationServiceException} is true), so that one also ends
 * as a server error.</li>
 * </ul>
 * <p>
 * Both are re-thrown as a {@link BadJwtException}, which is the one type that
 * reaches the client as a 401. Failures that are already BadJwtException are
 * passed through untouched, so a genuinely malformed or rejected token keeps its
 * own message — {@code JwtValidationException} (expiry, issuer, audience) is a
 * BadJwtException subclass, so validator failures are unaffected by this.
 * <p>
 * This is fail-closed: a token that cannot be verified never authenticates
 * anyone. The trade-off is that the response says "invalid_token" when the real
 * problem is at the IdP, so the true cause is logged at ERROR.
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
		} catch (BadJwtException e) {
			// Something about the token itself, not about reaching the IdP. Already a
			// 401, and its message is more useful than anything this class could add.
			throw e;
		} catch (JwtDecoderInitializationException | JwtException e) {
			// The caller is told "invalid_token", which is not the real reason, so the
			// real one has to be visible somewhere.
			log.error("Unable to reach the IdP to verify a bearer token; rejecting it as unverifiable", e);
			throw new BadJwtException(
				"Unable to verify the token: the IdP's configuration or signing keys could not be retrieved", e);
		}
	}
}
