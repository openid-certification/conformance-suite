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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
public class ApiTokenAuthenticationProvider implements AuthenticationProvider {

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

		Jwt fakeToken = createJwtFromApiToken(token, tokenInfoMap, tokenInfo, issuedAt, expiresAt);
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

	private Jwt createJwtFromApiToken(String token, Map<String, Object> tokenInfoMap, JsonObject tokenInfo,
									  Instant issuedAt, Instant expiresAt) {

		Map<String, Object> tokenClaims = new HashMap<>(tokenInfoMap);

		JsonObject ownerClaims = tokenInfo.getAsJsonObject("owner");
		if (ownerClaims == null || ownerClaims.get("iss") == null || ownerClaims.get("sub") == null) {
			// A token row with no usable owner cannot identify anyone. Returning null
			// rejects it as unauthenticated rather than throwing a NullPointerException
			// out of the filter chain as a 500.
			return null;
		}
		String iss = OIDFJSON.getString(ownerClaims.get("iss"));
		String sub = OIDFJSON.getString(ownerClaims.get("sub"));
		tokenClaims.put("iss", iss);
		tokenClaims.put("sub", sub);

		return new Jwt(token, issuedAt, expiresAt, Map.of("typ", "jwt"), tokenClaims);
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return BearerTokenAuthenticationToken.class.isAssignableFrom(authentication);
	}


}
