package net.openid.conformance.security;

import com.google.common.collect.ImmutableMap;
import net.openid.conformance.sharing.privatelink.PrivateLinkOneTimeToken;
import org.springframework.security.core.Authentication;

public interface AuthenticationFacade {

	boolean isAdmin();

	boolean isUser();

	boolean isPrivateLinkUser();

	PrivateLinkOneTimeToken getPrivateOneTimeToken();

	ImmutableMap<String, String> getPrincipal();

	String getDisplayName();

	void setLocalAuthentication(Authentication a);

	Authentication getContextAuthentication();

}
