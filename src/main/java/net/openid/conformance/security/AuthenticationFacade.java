package net.openid.conformance.security;

import com.google.common.collect.ImmutableMap;
import net.openid.conformance.support.mitre.compat.oidc.UserInfo;
import org.springframework.security.core.Authentication;

public interface AuthenticationFacade {

	boolean isAdmin();

	boolean isUser();

	ImmutableMap<String, String> getPrincipal();

	String getDisplayName();

	UserInfo getUserInfo();

	void setLocalAuthentication(Authentication a);

	Authentication getContextAuthentication();

}
