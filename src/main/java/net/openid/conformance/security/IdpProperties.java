package net.openid.conformance.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The OpenID Provider the suite trusts ({@code oidc.idp.*}). All of it is optional.
 * Setting the issuer alone does not make the API accept the IdP's access tokens as
 * bearer tokens: that needs {@code oidc.idp.accept-api-tokens=true} as well, and
 * without it the API accepts only the suite's own API tokens and share links.
 *
 * @param iss                    the issuer identifier; required when IdP access
 *                               tokens are accepted
 * @param apiAudience            the value an access token's {@code aud} must
 *                               contain; required when IdP access tokens are
 *                               accepted
 * @param accessTokenSigningAlgs the JWS algorithms the IdP signs access tokens
 *                               with, comma separated; blank means
 *                               {@link IdpJwsAlgorithms#DEFAULT}
 */
@ConfigurationProperties("oidc.idp")
public record IdpProperties(String iss, String apiAudience, String accessTokenSigningAlgs) {

	public static final String ISSUER_PROPERTY = "oidc.idp.iss";

	/** Whether the API accepts access tokens issued by {@link #iss} as bearer tokens; false unless set to true. */
	public static final String ACCEPT_API_TOKENS_PROPERTY = "oidc.idp.accept-api-tokens";
}
