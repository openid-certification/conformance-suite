package io.fintechlabs.testframework.security;

import java.util.Map;

import org.mitre.openid.connect.model.OIDCAuthenticationToken;
import org.mitre.openid.connect.model.UserInfo;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;

@Component
public class OIDCAuthenticationFacade implements AuthenticationFacade{
	@Override
	public OIDCAuthenticationToken getAuthenticationToken() {
		Authentication a = SecurityContextHolder.getContext().getAuthentication();
		if (a instanceof OIDCAuthenticationToken) {
			return (OIDCAuthenticationToken)a;
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
		OIDCAuthenticationToken token = getAuthenticationToken();
		if (token != null) {
			return (ImmutableMap<String,String>)token.getPrincipal();
		}
		return null;
	}

	@Override
	public String getDisplayName(){
		OIDCAuthenticationToken token = getAuthenticationToken();
		if (token != null) {
			Map<String, String> principal = getPrincipal();
			if (principal != null) {
				String displayName = principal.toString();
				UserInfo userInfo = token.getUserInfo();
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
		}
		return "";
	}
}
