package net.openid.conformance.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

/**
 * Rejects IdP-issued access tokens that were not minted for this client.
 * <p>
 * Verifying the issuer and signature alone is not enough. Every access token the
 * IdP issues to <em>any</em> of its clients — including a public client a user can
 * obtain a token from themselves — carries the same issuer and is signed by the
 * same keys. Without this check, such a token authenticates its bearer here and is
 * granted ROLE_USER over the whole {@code /api/**} surface, with its (iss, sub)
 * becoming the ownership principal: a confused deputy that turns any other client
 * of the realm into a way in.
 * <p>
 * A token counts as ours if either:
 * <ul>
 * <li>{@code aud} contains our client id — the audience restriction of
 * RFC 9068 §3, and what an IdP configured with an audience mapper emits; or</li>
 * <li>{@code azp} equals our client id — the "authorized party", i.e. the client
 * the token was issued to.</li>
 * </ul>
 * The {@code azp} arm is not a loosening: both claims are set by the IdP and both
 * say the token was minted for this client, which is exactly the property being
 * checked. It is there because putting the client id in {@code aud} is not
 * universal — several OPs audience their access tokens at a resource instead
 * (Keycloak's default is {@code "account"}) and identify the client only in
 * {@code azp} — so an {@code aud}-only check would lock out every API caller of
 * such a deployment.
 * <p>
 * Note this deliberately does not also require a scope: the suite's API is
 * authorized by role, and the IdP does not issue a suite-specific scope.
 */
public final class IdpAudienceValidator implements OAuth2TokenValidator<Jwt> {

	private static final String AZP = "azp";

	private final String clientId;

	public IdpAudienceValidator(String clientId) {
		this.clientId = clientId;
	}

	@Override
	public OAuth2TokenValidatorResult validate(Jwt token) {
		if (isForThisClient(token)) {
			return OAuth2TokenValidatorResult.success();
		}
		// The reason is deliberately generic in the response - the caller does not
		// need to be told which client the suite is - but names both claims so that
		// an operator whose IdP populates neither can see what to configure.
		return OAuth2TokenValidatorResult.failure(new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN,
			"The token was not issued for this application: neither its 'aud' nor its 'azp' claim identifies this client",
			null));
	}

	private boolean isForThisClient(Jwt token) {
		// A blank client id would make every token match on a null/blank claim,
		// silently disabling the check that this class exists to perform.
		if (clientId == null || clientId.isBlank()) {
			return false;
		}
		List<String> audience = token.getAudience();
		if (audience != null && audience.contains(clientId)) {
			return true;
		}
		return clientId.equals(token.getClaimAsString(AZP));
	}
}
