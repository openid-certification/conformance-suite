package net.openid.conformance.token;

import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import net.openid.conformance.testmodule.OIDFJSON;
import org.mitre.openid.connect.model.DefaultUserInfo;
import org.mitre.openid.connect.model.OIDCAuthenticationToken;
import org.mitre.openid.connect.model.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;

import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

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
				!token.isExpired(),
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

	private static class LocalOAuth2AccessToken implements OAuth2AccessToken {

		private String tokenString;
		private Date expireDate;

		public LocalOAuth2AccessToken(JsonObject info) {
			this.tokenString = OIDFJSON.getString(info.get("token"));

			JsonPrimitive expires = info.getAsJsonPrimitive("expires");
			this.expireDate = expires != null ? new Date(OIDFJSON.getLong(expires)) : null;
		}

		@Override
		public Map<String, Object> getAdditionalInformation() {
			return null;
		}

		@Override
		public Set<String> getScope() {
			return Collections.emptySet();
		}

		@Override
		public OAuth2RefreshToken getRefreshToken() {
			return null;
		}

		@Override
		public String getTokenType() {
			return BEARER_TYPE;
		}

		@Override
		public boolean isExpired() {
			return expireDate != null && expireDate.before(new Date());
		}

		@Override
		public Date getExpiration() {
			return expireDate;
		}

		@Override
		public int getExpiresIn() {
			if (expireDate != null) {
				return (int)TimeUnit.MILLISECONDS.toSeconds(expireDate.getTime() - (new Date()).getTime());
			}
			return 0;
		}

		@Override
		public String getValue() {
			return tokenString;
		}

	}

}
