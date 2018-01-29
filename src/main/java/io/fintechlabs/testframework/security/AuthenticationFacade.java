package io.fintechlabs.testframework.security;

import org.mitre.openid.connect.model.OIDCAuthenticationToken;

import com.google.common.collect.ImmutableMap;

public interface AuthenticationFacade {
	OIDCAuthenticationToken getAuthenticationToken();
	boolean isAdmin();
	ImmutableMap<String,String> getPrincipal();
	String getDisplayName();
}
