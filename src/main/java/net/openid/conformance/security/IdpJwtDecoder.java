package net.openid.conformance.security;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtAudienceValidator;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.web.client.RestTemplate;

import java.text.ParseException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Decodes and validates access tokens issued by the IdP, for use as API bearer
 * tokens.
 * <p>
 * Every bearer token the share-link and API-token providers do not recognise
 * reaches this decoder, including from anonymous callers, so it is written to keep
 * that from reaching the IdP or the logs more than necessary:
 * <ul>
 * <li>A token whose unverified {@code iss} is not the IdP is rejected without any
 * network call.</li>
 * <li>The IdP's discovery document is fetched on the first token that needs it, not
 * at startup, so the suite starts while the IdP is unreachable. A failed fetch is
 * remembered for {@link #RETRY_AFTER_FAILURE}; tokens arriving in that window are
 * rejected at once, and only the failed attempt is logged.</li>
 * <li>A failure to fetch the signing keys is logged at ERROR at most once per
 * {@link #RETRY_AFTER_FAILURE}.</li>
 * <li>Every rejection reaches the caller as a {@link BadJwtException} with the same
 * message, which JwtAuthenticationProvider turns into a 401. The reason (expiry,
 * audience, IdP outage) is logged at DEBUG, or ERROR for an outage, and is not sent
 * to the caller.</li>
 * </ul>
 */
public final class IdpJwtDecoder implements JwtDecoder {

	static final Duration RETRY_AFTER_FAILURE = Duration.ofSeconds(30);

	static final String REJECTED = "Invalid bearer token";

	private static final Duration IDP_CONNECT_TIMEOUT = Duration.ofSeconds(5);

	private static final Duration IDP_READ_TIMEOUT = Duration.ofSeconds(5);

	private static final Logger log = LoggerFactory.getLogger(IdpJwtDecoder.class);

	private final String issuer;

	private final Supplier<JwtDecoder> delegateFactory;

	private final LongSupplier nanoTime;

	private final long retryAfterFailureNanos;

	// A lock rather than synchronized: discovery is a network call made while it is
	// held, and on Java 21 a virtual thread blocked inside synchronized pins its
	// carrier thread.
	private final ReentrantLock resolveLock = new ReentrantLock();

	private volatile JwtDecoder delegate;

	// Guarded by resolveLock.
	private boolean discoveryFailed;

	// Guarded by resolveLock.
	private long discoveryFailedAt;

	private final AtomicLong lastKeyFailureLoggedAt = new AtomicLong();

	private volatile boolean keyFailureLogged;

	IdpJwtDecoder(String issuer, Supplier<JwtDecoder> delegateFactory, LongSupplier nanoTime,
		Duration retryAfterFailure) {
		this.issuer = issuer;
		this.delegateFactory = delegateFactory;
		this.nanoTime = nanoTime;
		this.retryAfterFailureNanos = retryAfterFailure.toNanos();
	}

	/**
	 * Does not contact the IdP: its discovery document is fetched on first use.
	 *
	 * @throws IllegalStateException if the issuer is set without an API audience, or
	 *                               the signing algorithms are invalid, so that the
	 *                               suite fails at startup rather than starting with
	 *                               an IdP whose tokens it cannot check
	 */
	public static IdpJwtDecoder forIssuer(IdpProperties idp) {
		String issuer = idp.iss();
		String audience = idp.apiAudience();
		if (audience == null || audience.isBlank()) {
			throw new IllegalStateException("oidc.idp.api-audience (environment variable OIDC_IDP_API_AUDIENCE) is"
				+ " not set. Set it to the audience the IdP " + issuer + " puts in the 'aud' claim of access"
				+ " tokens for this API, or unset oidc.idp.iss (OIDC_IDP_ISS) to accept no IdP access tokens.");
		}
		List<SignatureAlgorithm> algorithms = IdpJwsAlgorithms.parse(idp.accessTokenSigningAlgs());
		return new IdpJwtDecoder(issuer, () -> buildDelegate(issuer, audience, algorithms), System::nanoTime,
			RETRY_AFTER_FAILURE);
	}

	/**
	 * The checks every IdP token must pass once its signature has been verified
	 * against the IdP's keys.
	 * <p>
	 * The list is built explicitly rather than with
	 * {@code JwtValidators.createDefaultWithIssuer}: that one adds
	 * {@code JwtTypeValidator.jwt()}, which rejects a {@code typ} header of
	 * {@code at+jwt}, and accepts tokens with no {@code exp}.
	 */
	public static OAuth2TokenValidator<Jwt> accessTokenValidator(String issuer, String audience) {
		JwtTimestampValidator timestamps = new JwtTimestampValidator();
		timestamps.setAllowEmptyExpiryClaim(false);
		return new DelegatingOAuth2TokenValidator<>(List.of(
			new IdpAccessTokenTypeValidator(),
			timestamps,
			new JwtIssuerValidator(issuer),
			new JwtAudienceValidator(audience),
			// The suite identifies users by (iss, sub). A token without a subject would
			// authenticate and then fail when the principal is read.
			new JwtClaimValidator<Object>(JwtClaimNames.SUB, sub -> sub instanceof String s && !s.isBlank())));
	}

	static NimbusJwtDecoder buildDelegate(String issuer, String audience, List<SignatureAlgorithm> algorithms) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(IDP_CONNECT_TIMEOUT);
		requestFactory.setReadTimeout(IDP_READ_TIMEOUT);

		// Every accepted algorithm is asymmetric and its key is resolved from the
		// IdP's JWKS by kid, so accepting more than one does not allow algorithm
		// confusion. The typ header is checked by IdpAccessTokenTypeValidator, not
		// by Nimbus, which would accept only "JWT".
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withIssuerLocation(issuer)
			.restOperations(new RestTemplate(requestFactory))
			.validateType(false)
			.jwsAlgorithms(configured -> configured.addAll(algorithms))
			.build();
		decoder.setJwtValidator(accessTokenValidator(issuer, audience));
		return decoder;
	}

	@Override
	public Jwt decode(String token) throws JwtException {
		if (!claimsToBeFromIssuer(token)) {
			throw new BadJwtException(REJECTED);
		}
		JwtDecoder resolved = resolveDelegate();
		try {
			return resolved.decode(token);
		} catch (BadJwtException e) {
			// Something about the token itself: a bad signature, or a failed check
			// from accessTokenValidator.
			log.debug("Rejected an IdP bearer token: {}", e.getMessage());
			throw new BadJwtException(REJECTED, e);
		} catch (JwtException e) {
			// NimbusJwtDecoder throws a plain JwtException when it cannot fetch the
			// signing keys. JwtAuthenticationProvider would turn that into an
			// AuthenticationServiceException, which the filter chain re-throws as a
			// server error.
			logKeyFailure(e);
			throw new BadJwtException(REJECTED, e);
		}
	}

	private boolean claimsToBeFromIssuer(String token) {
		try {
			JWT jwt = JWTParser.parse(token);
			return jwt instanceof SignedJWT && issuer.equals(jwt.getJWTClaimsSet().getIssuer());
		} catch (ParseException e) {
			return false;
		}
	}

	private JwtDecoder resolveDelegate() {
		JwtDecoder resolved = delegate;
		if (resolved != null) {
			return resolved;
		}
		resolveLock.lock();
		try {
			if (delegate != null) {
				return delegate;
			}
			long now = nanoTime.getAsLong();
			if (discoveryFailed && now - discoveryFailedAt < retryAfterFailureNanos) {
				throw new BadJwtException(REJECTED);
			}
			try {
				delegate = delegateFactory.get();
				discoveryFailed = false;
				return delegate;
			} catch (RuntimeException e) {
				discoveryFailed = true;
				discoveryFailedAt = now;
				log.error("Unable to fetch the discovery document of the IdP {}; IdP bearer tokens are rejected"
					+ " until a retry in {} seconds succeeds", issuer, RETRY_AFTER_FAILURE.toSeconds(), e);
				throw new BadJwtException(REJECTED, e);
			}
		} finally {
			resolveLock.unlock();
		}
	}

	private void logKeyFailure(JwtException e) {
		long now = nanoTime.getAsLong();
		long last = lastKeyFailureLoggedAt.get();
		boolean due = !keyFailureLogged || now - last >= retryAfterFailureNanos;
		if (due && lastKeyFailureLoggedAt.compareAndSet(last, now)) {
			keyFailureLogged = true;
			log.error("Unable to fetch the signing keys of the IdP {} to verify a bearer token; further failures"
				+ " are logged at DEBUG for the next {} seconds", issuer, RETRY_AFTER_FAILURE.toSeconds(), e);
		} else {
			log.debug("Unable to fetch the signing keys of the IdP {}: {}", issuer, e.getMessage());
		}
	}
}
