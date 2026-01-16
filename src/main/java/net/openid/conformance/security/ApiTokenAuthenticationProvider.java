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

		Set<GrantedAuthority> authorities = new HashSet<>();
		authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
		Jwt fakeToken = createJwtFromApiToken(token, tokenInfoMap);
		return new JwtAuthenticationToken(fakeToken, authorities);
	}

	private Jwt createJwtFromApiToken(String token, Map<String, Object> tokenInfoMap) {

		JsonObject tokenInfo = (JsonObject) new Gson().toJsonTree(tokenInfoMap);

		Map<String, Object> tokenClaims = new HashMap<>();
		tokenClaims.putAll(tokenInfoMap);

		JsonObject ownerClaims = tokenInfo.getAsJsonObject("owner");
		String iss = OIDFJSON.getString(ownerClaims.get("iss"));
		String sub = OIDFJSON.getString(ownerClaims.get("sub"));
		tokenClaims.put("iss", iss);
		tokenClaims.put("sub", sub);

		Instant instantAt = Instant.now();
		JsonPrimitive expires = tokenInfo.getAsJsonPrimitive("expires");
		Instant expiresAt = expires != null ? Instant.ofEpochMilli(OIDFJSON.getLong(tokenInfo.getAsJsonPrimitive("expires"))) : null;

		return new Jwt(token,instantAt,expiresAt, Map.of("typ", "jwt"),tokenClaims);
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return BearerTokenAuthenticationToken.class.isAssignableFrom(authentication);
	}


}
