package net.openid.conformance.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Accepts only JWTs that identify themselves as access tokens, so that an ID token
 * (or a logout or refresh token) the IdP signed for the same client cannot be
 * presented as an API bearer token.
 * <p>
 * Accepted:
 * <ul>
 * <li>a {@code typ} header of {@code at+jwt} or {@code application/at+jwt}, the
 * RFC 9068 section 2.1 marking for JWT access tokens;</li>
 * <li>a {@code typ} claim of {@code Bearer} under a {@code JWT} or absent header,
 * which is how Keycloak marks its access tokens unless the client is configured to
 * emit {@code at+jwt};</li>
 * <li>a {@code typ} header of {@code JWT} with no {@code typ} claim, for IdPs that
 * do not mark their access tokens at all.</li>
 * </ul>
 * A {@code typ} claim other than {@code Bearer} is always rejected: Keycloak sets
 * {@code ID}, {@code Logout} and {@code Refresh} on its other token types under a
 * {@code JWT} header. A token with no type marking at all, the usual shape of an
 * OIDC ID token, is rejected too.
 * <p>
 * The third case cannot tell an access token from an ID token whose header says
 * {@code JWT}. Such an ID token is still rejected by the audience check, because
 * its {@code aud} is the client id, not {@code oidc.idp.api-audience}; that only
 * holds while the two differ.
 * <p>
 * Header values are compared case-insensitively (RFC 7515 section 4.1.9), and so is
 * the claim, as OAuth token type names are (RFC 6749 section 5.1).
 */
public final class IdpAccessTokenTypeValidator implements OAuth2TokenValidator<Jwt> {

	private static final String TYP = "typ";

	@Override
	public OAuth2TokenValidatorResult validate(Jwt token) {
		Object headerType = token.getHeaders().get(TYP);
		if (isAtJwt(headerType)) {
			return OAuth2TokenValidatorResult.success();
		}
		Object claimType = token.getClaims().get(TYP);
		if (claimType == null) {
			if (isJwt(headerType)) {
				return OAuth2TokenValidatorResult.success();
			}
		} else if ((headerType == null || isJwt(headerType))
			&& claimType instanceof String type && "Bearer".equalsIgnoreCase(type)) {
			return OAuth2TokenValidatorResult.success();
		}
		return OAuth2TokenValidatorResult.failure(new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN,
			"The token is not an access token: expected a 'typ' header of 'at+jwt' or 'JWT', and no 'typ' claim"
				+ " other than 'Bearer'",
			null));
	}

	private static boolean isAtJwt(Object headerType) {
		return headerType instanceof String type
			&& ("at+jwt".equalsIgnoreCase(type) || "application/at+jwt".equalsIgnoreCase(type));
	}

	private static boolean isJwt(Object headerType) {
		return headerType instanceof String type && "JWT".equalsIgnoreCase(type);
	}
}
