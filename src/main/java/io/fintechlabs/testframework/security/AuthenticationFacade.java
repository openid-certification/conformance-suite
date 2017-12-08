package io.fintechlabs.testframework.security;

import com.google.common.collect.ImmutableMap;
import org.mitre.openid.connect.model.OIDCAuthenticationToken;

public interface AuthenticationFacade {
	OIDCAuthenticationToken getAuthenticationToken();
	boolean isAdmin();
	ImmutableMap<String,String> getPrincipal();
}
