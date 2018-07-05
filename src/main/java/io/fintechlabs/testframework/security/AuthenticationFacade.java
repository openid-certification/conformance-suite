package io.fintechlabs.testframework.security;

import org.mitre.openid.connect.model.UserInfo;

import com.google.common.collect.ImmutableMap;

public interface AuthenticationFacade {

	boolean isAdmin();

	ImmutableMap<String, String> getPrincipal();

	String getDisplayName();
	
	UserInfo getUserInfo();
	
}
