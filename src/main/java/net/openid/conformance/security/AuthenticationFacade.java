package net.openid.conformance.security;

import com.google.common.collect.ImmutableMap;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;

public interface AuthenticationFacade {

	boolean isAdmin();

	boolean isUser();

	ImmutableMap<String, String> getPrincipal();

	String getDisplayName();

	OidcUserInfo getUserInfo();

	void setLocalAuthentication(Authentication a);

	Authentication getContextAuthentication();

}
