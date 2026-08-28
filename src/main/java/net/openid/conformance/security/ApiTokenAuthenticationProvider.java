package net.openid.conformance.security;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.token.TokenService;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
public class ApiTokenAuthenticationProvider implements AuthenticationProvider {

	/**
	 * Stands in for the presented secret in the principal's token value. Non-empty
	 * because Jwt rejects a blank one.
	 */
	private static final String PLACEHOLDER_TOKEN_VALUE = "api-token";

	private final TokenService tokenService;

	public ApiTokenAuthenticationProvider(TokenService tokenService) {
		this.tokenService = tokenService;
	}

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {

		Object maybeToken = authentication.getCredentials();
		if (!(maybeToken instanceof String token)) {
			return null;
		}

		var tokenInfoMap = tokenService.findToken(token);
		if (tokenInfoMap == null) {
			return null;
		}

		JsonObject tokenInfo = (JsonObject) new Gson().toJsonTree(tokenInfoMap);

		// The expiry check must come before building the Jwt: the returned
		// JwtAuthenticationToken is authenticated from construction and never
		// re-checks expiry (the removed ApiTokenAuthenticationToken did this in
		// isAuthenticated()), and the Jwt constructor itself rejects an expiry
		// that precedes its issuedAt, so building it first turns an expired
		// token into an IllegalArgumentException/500 instead of a 401.
		// Returning null lets ProviderManager fall through to the next provider.
		Instant issuedAt = Instant.now();
		Instant expiresAt = expiresAt(tokenInfo);
		if (expiresAt != null && !expiresAt.isAfter(issuedAt)) {
			return null;
		}

		Jwt fakeToken = createJwtFromApiToken(tokenInfo, issuedAt, expiresAt);
		if (fakeToken == null) {
			return null;
		}

		Set<GrantedAuthority> authorities = new HashSet<>();
		authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
		return new JwtAuthenticationToken(fakeToken, authorities);
	}

	private Instant expiresAt(JsonObject tokenInfo) {
		JsonPrimitive expires = tokenInfo.getAsJsonPrimitive("expires");
		return expires != null ? Instant.ofEpochMilli(OIDFJSON.getLong(expires)) : null;
	}

	/**
	 * Builds the principal for an accepted API token.
	 * <p>
	 * Deliberately carries the owner's identity and nothing else. The token's own
	 * database row must not be copied in wholesale: it holds the token secret under
	 * "token", and everything in a Jwt's claims is reachable from the security
	 * context and liable to be logged or serialized. For the same reason the token
	 * value is a placeholder rather than the presented secret — a Jwt's token value
	 * is the string a resource server would forward onwards, and this one is an
	 * opaque credential that nothing downstream should ever see. (The pre-IdP code
	 * used the literal "dummy" here, and exposed only iss and sub.)
	 */
	private Jwt createJwtFromApiToken(JsonObject tokenInfo, Instant issuedAt, Instant expiresAt) {

		JsonObject ownerClaims = tokenInfo.getAsJsonObject("owner");
		if (ownerClaims == null || ownerClaims.get("iss") == null || ownerClaims.get("sub") == null) {
			// A token row with no usable owner cannot identify anyone. Returning null
			// rejects it as unauthenticated rather than throwing a NullPointerException
			// out of the filter chain as a 500.
			return null;
		}
		Map<String, Object> tokenClaims = Map.of(
			"iss", OIDFJSON.getString(ownerClaims.get("iss")),
			"sub", OIDFJSON.getString(ownerClaims.get("sub")));

		return new Jwt(PLACEHOLDER_TOKEN_VALUE, issuedAt, expiresAt, Map.of("typ", "jwt"), tokenClaims);
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return BearerTokenAuthenticationToken.class.isAssignableFrom(authentication);
	}


}
