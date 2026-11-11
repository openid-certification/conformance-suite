package net.openid.conformance.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The OpenID Provider whose access tokens the API accepts as bearer tokens
 * ({@code oidc.idp.*}). All of it is optional: without an issuer the API accepts
 * only the suite's own API tokens and share links.
 *
 * @param iss                    the issuer identifier; blank leaves IdP tokens out
 *                               of the API
 * @param apiAudience            the value an access token's {@code aud} must
 *                               contain; required once {@code iss} is set
 * @param accessTokenSigningAlgs the JWS algorithms the IdP signs access tokens
 *                               with, comma separated; blank means
 *                               {@link IdpJwsAlgorithms#DEFAULT}
 */
@ConfigurationProperties("oidc.idp")
public record IdpProperties(String iss, String apiAudience, String accessTokenSigningAlgs) {

	public static final String ISSUER_PROPERTY = "oidc.idp.iss";
}
