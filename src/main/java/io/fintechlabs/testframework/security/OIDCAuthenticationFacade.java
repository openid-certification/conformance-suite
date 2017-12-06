package io.fintechlabs.testframework.security;

import org.mitre.openid.connect.model.OIDCAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

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
}
