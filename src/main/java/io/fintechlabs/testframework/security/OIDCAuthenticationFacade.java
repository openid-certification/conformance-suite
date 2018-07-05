package io.fintechlabs.testframework.security;

import java.util.Map;

import org.mitre.openid.connect.model.OIDCAuthenticationToken;
import org.mitre.openid.connect.model.UserInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;

@Component
public class OIDCAuthenticationFacade implements AuthenticationFacade {

	@Value("${oauth.introspection_url}")
	private String introspectionUrl;
	
	private OIDCAuthenticationToken getOIDC() {
		Authentication a = SecurityContextHolder.getContext().getAuthentication();
		if (a instanceof OIDCAuthenticationToken) {
			return (OIDCAuthenticationToken) a;
		}
		return null;
	}
	
	private OAuth2Authentication getOAuth() {
		Authentication a = SecurityContextHolder.getContext().getAuthentication();
		if (a instanceof OAuth2Authentication) {
			return (OAuth2Authentication) a;
		}
		return null;
	}

	/**
	 * Check to see if the current logged in user has the ROLE_ADMIN authority defined in
	 * GoogleHostedDomainAdminAuthoritiesMapper
	 *
	 * TODO: Probably should move the ROLES to a different static class.
	 *
	 * @return
	 */
	@Override
	public boolean isAdmin() {
		Authentication a = SecurityContextHolder.getContext().getAuthentication();
		if (a != null) {
			return a.getAuthorities().contains(GoogleHostedDomainAdminAuthoritiesMapper.ROLE_ADMIN);
		}
		return false;
	}

	@Override
	public ImmutableMap<String, String> getPrincipal() {
		OIDCAuthenticationToken token = getOIDC();
		OAuth2Authentication auth = getOAuth();
		if (token != null) {
			return (ImmutableMap<String, String>) token.getPrincipal();
		} else if (auth != null) {
			return ImmutableMap.of("sub", auth.getOAuth2Request().getClientId(), "iss", introspectionUrl);
		}
		return null;
	}

	@Override
	public String getDisplayName() {
		OIDCAuthenticationToken token = getOIDC();
		OAuth2Authentication auth = getOAuth();
		if (token != null) {
			Map<String, String> principal = getPrincipal();
			if (principal != null) {
				String displayName = principal.toString();
				UserInfo userInfo = getUserInfo();
				if (userInfo != null) {
					if (!Strings.isNullOrEmpty(userInfo.getEmail())) {
						displayName = userInfo.getEmail();
					} else if (!Strings.isNullOrEmpty(userInfo.getPreferredUsername())) {
						displayName = userInfo.getPreferredUsername();
					} else if (!Strings.isNullOrEmpty(userInfo.getName())) {
						displayName = userInfo.getName();
					}
					return displayName;
				}
				return displayName;
			}
		} else if (auth != null) {
			return auth.getName();
		}
		return "";
	}

	@Override
	public UserInfo getUserInfo() {
		OIDCAuthenticationToken token = getOIDC();
		
		if (token != null) {
			return token.getUserInfo();
		} else {
			return null;
		}
		
	}
}
