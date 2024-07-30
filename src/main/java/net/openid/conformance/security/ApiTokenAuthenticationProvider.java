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
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.stereotype.Component;

import java.io.Serial;
import java.time.Instant;
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
		@SuppressWarnings("unchecked") DefaultOidcUser oidcUser = createOidcUserFromApiToken(tokenInfoMap, authorities);
		return new ApiTokenAuthenticationToken(oidcUser, authorities);
	}

	private DefaultOidcUser createOidcUserFromApiToken(Map<String, Object> tokenInfoMap, Set<GrantedAuthority> authorities) {

		JsonObject tokenInfo = (JsonObject) new Gson().toJsonTree(tokenInfoMap);

		JsonObject ownerClaims = tokenInfo.getAsJsonObject("owner");
		String iss = OIDFJSON.getString(ownerClaims.get("iss"));
		String sub = OIDFJSON.getString(ownerClaims.get("sub"));
		Map<String, Object> idTokenClaims = Map.of("iss", iss, "sub", sub);
		Instant instantAt = Instant.now();
		JsonPrimitive expires = tokenInfo.getAsJsonPrimitive("expires");
		Instant expiresAt = expires != null ? Instant.ofEpochMilli(OIDFJSON.getLong(tokenInfo.getAsJsonPrimitive("expires"))) : null;
		OidcIdToken idToken = new OidcIdToken("dummy", instantAt, expiresAt, idTokenClaims);

		OidcUserInfo oidcUserInfo = new OidcUserInfo(idTokenClaims);

		DefaultOidcUser oidcUser = new DefaultOidcUser(authorities, idToken, oidcUserInfo);
		authorities.add(new OidcUserAuthority(idToken, oidcUserInfo));
		return oidcUser;
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return BearerTokenAuthenticationToken.class.isAssignableFrom(authentication);
	}

	public static class ApiTokenAuthenticationToken extends OAuth2AuthenticationToken {

		@Serial
		private static final long serialVersionUID = 1L;

		private final DefaultOidcUser oidcUser;

		public ApiTokenAuthenticationToken(DefaultOidcUser oidcUser, Set<GrantedAuthority> authorities) {
			super(oidcUser,authorities, "dummy");
			this.oidcUser = oidcUser;
		}

		@Override
		public boolean isAuthenticated() {

			if (oidcUser.getIdToken().getExpiresAt() == null) {
				return true;
			}

			return oidcUser.getIdToken().getExpiresAt().isAfter(Instant.now());
		}
	}
}
