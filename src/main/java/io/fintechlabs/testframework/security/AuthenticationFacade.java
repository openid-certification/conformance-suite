package io.fintechlabs.testframework.security;

import org.mitre.openid.connect.model.UserInfo;
import org.springframework.security.core.Authentication;

import com.google.common.collect.ImmutableMap;

public interface AuthenticationFacade {

	boolean isAdmin();

	boolean isUser();

	ImmutableMap<String, String> getPrincipal();

	String getDisplayName();

	UserInfo getUserInfo();

	void setLocalAuthentication(Authentication a);

	Authentication getContextAuthentication();

}
