package io.fintechlabs.testframework.security;

import org.mitre.openid.connect.model.OIDCAuthenticationToken;

public interface AuthenticationFacade {
	OIDCAuthenticationToken getAuthenticationToken();
	boolean isAdmin();
}
