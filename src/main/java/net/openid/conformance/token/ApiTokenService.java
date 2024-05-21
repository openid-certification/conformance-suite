package net.openid.conformance.token;

import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.support.mitre.compat.introspect.ResourceServerTokenServices;
import net.openid.conformance.support.mitre.compat.oidc.DefaultUserInfo;
import net.openid.conformance.support.mitre.compat.oidc.UserInfo;
import net.openid.conformance.support.mitre.compat.spring.OIDCAuthenticationToken;
import net.openid.conformance.testmodule.OIDFJSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;

import java.io.Serial;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("deprecation")
public class ApiTokenService implements ResourceServerTokenServices {

	@Autowired
	private TokenService tokenService;

	private ResourceServerTokenServices fallbackService;

	public void setFallbackService(ResourceServerTokenServices service) {
		fallbackService = service;
	}

	@SuppressWarnings("rawtypes")
	private TokenInfo checkToken(String accessToken) {

		Map tokenInfoMap = tokenService.findToken(accessToken);
		if (tokenInfoMap == null) {
			return null;
		}

		JsonObject tokenInfoObj = (JsonObject) new Gson().toJsonTree(tokenInfoMap);

		OAuth2AccessToken token = new LocalOAuth2AccessToken(tokenInfoObj);

		OAuth2Request request = new OAuth2Request(
			Collections.emptyMap(),
			"",
			null,
			!token.getExpiresAt().isBefore(Instant.now()),
			Collections.emptySet(),
			null,
			null,
			null,
			null);

		OAuth2Authentication auth = new OAuth2Authentication(request, createAuth(tokenInfoObj));

		return new TokenInfo(token, auth);
	}

	private Authentication createAuth(JsonObject tokenInfo) {

		JsonObject owner = tokenInfo.getAsJsonObject("owner");
		String iss = OIDFJSON.getString(owner.get("iss"));
		String sub = OIDFJSON.getString(owner.get("sub"));

		Set<GrantedAuthority> authorities = ImmutableSet.of(new SimpleGrantedAuthority("ROLE_USER"));
		UserInfo info = DefaultUserInfo.fromJson(tokenInfo.getAsJsonObject("info"));
		return new OIDCAuthenticationToken(sub, iss, info, authorities, null, null, null);
	}

	@Override
	public OAuth2Authentication loadAuthentication(String accessToken) throws AuthenticationException, InvalidTokenException {

		TokenInfo info = checkToken(accessToken);
		if (info != null) {
			return info.auth;
		} else if (fallbackService != null) {
			return fallbackService.loadAuthentication(accessToken);
		} else {
			return null;
		}
	}

	@Override
	public OAuth2AccessToken readAccessToken(String accessToken) {

		TokenInfo info = checkToken(accessToken);
		if (info != null) {
			return info.token;
		} else if (fallbackService != null) {
			return fallbackService.readAccessToken(accessToken);
		} else {
			return null;
		}
	}

	private static class TokenInfo {

		OAuth2AccessToken token;
		OAuth2Authentication auth;

		public TokenInfo(OAuth2AccessToken token, OAuth2Authentication auth) {
			this.token = token;
			this.auth = auth;
		}
	}

	private static class LocalOAuth2AccessToken extends OAuth2AccessToken {

		@Serial
		private static final long serialVersionUID = 1L;

		public LocalOAuth2AccessToken(JsonObject info) {
			super(TokenType.BEARER, OIDFJSON.getString(info.get("token")), null, extractExpiresAt(info));
		}

		private static Instant extractExpiresAt(JsonObject info) {
			JsonPrimitive expires = info.getAsJsonPrimitive("expires");
			return expires != null ? Instant.ofEpochMilli(OIDFJSON.getLong(expires)) : null;
		}

	}

}
