package net.openid.conformance.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Map;

public interface AuthenticationFacade {

	boolean isAdmin();

	boolean isUser();

	Map<String, String> getPrincipal();

	String getDisplayName();

	OAuth2User getUserInfo();

	void setLocalAuthentication(Authentication a);

	Authentication getContextAuthentication();

}
