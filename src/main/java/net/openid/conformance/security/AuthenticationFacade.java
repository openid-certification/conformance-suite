package net.openid.conformance.security;

import com.google.common.collect.ImmutableMap;
import org.springframework.security.core.Authentication;

public interface AuthenticationFacade {

	boolean isAdmin();

	boolean isUser();

	ImmutableMap<String, String> getPrincipal();

	String getDisplayName();

	void setLocalAuthentication(Authentication a);

	Authentication getContextAuthentication();

}
