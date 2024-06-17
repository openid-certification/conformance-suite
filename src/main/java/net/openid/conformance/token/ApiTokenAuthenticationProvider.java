package net.openid.conformance.token;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.testmodule.OIDFJSON;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.stereotype.Component;

import java.io.Serial;
import java.time.Instant;
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

		Set<GrantedAuthority> authorities = Set.of(new SimpleGrantedAuthority("ROLE_USER"));
		@SuppressWarnings("unchecked")
		OidcUser oidcUser = createOidcUserFromApiToken(tokenInfoMap, authorities);
		return new ApiTokenAuthenticationToken(token, oidcUser, authorities);
	}

	private OidcUser createOidcUserFromApiToken(Map<String, Object> tokenInfoMap, Set<GrantedAuthority> authorities) {

		JsonObject tokenInfo = (JsonObject) new Gson().toJsonTree(tokenInfoMap);
		var oidcUserInfoBuilder = OidcUserInfo.builder();
		JsonObject userInfoObject = tokenInfo.getAsJsonObject("info");
		for (var entry : userInfoObject.entrySet()) {
			oidcUserInfoBuilder.claim(entry.getKey(), entry.getValue());
		}
		OidcUserInfo oidcUserInfo = oidcUserInfoBuilder.build();

		JsonObject ownerClaims = tokenInfo.getAsJsonObject("owner");
		String iss = OIDFJSON.getString(ownerClaims.get("iss"));
		String sub = OIDFJSON.getString(ownerClaims.get("sub"));

		Instant instantAt = Instant.now();
		JsonPrimitive expires = tokenInfo.getAsJsonPrimitive("expires");
		Instant expiresAt = expires != null ? Instant.ofEpochMilli(OIDFJSON.getLong(tokenInfo.getAsJsonPrimitive("expires"))) : null;
		Map<String, Object> idTokenClaims = Map.of("iss", iss, "sub", sub);
		OidcUser oidcUser = new DefaultOidcUser(authorities, new OidcIdToken("dummy", instantAt, expiresAt, idTokenClaims), oidcUserInfo);

		return oidcUser;
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return BearerTokenAuthenticationToken.class.isAssignableFrom(authentication);
	}

	public static class ApiTokenAuthenticationToken extends AbstractAuthenticationToken {

		@Serial
		private static final long serialVersionUID = 1L;

		private final String token;

		private final OidcUser oidcUser;

		public ApiTokenAuthenticationToken(String token, OidcUser oidcUser, Set<GrantedAuthority> authorities) {
			super(authorities);
			this.token = token;
			this.oidcUser = oidcUser;
		}

		@Override
		public Object getPrincipal() {
			return oidcUser;
		}

		@Override
		public Object getCredentials() {
			return token;
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
