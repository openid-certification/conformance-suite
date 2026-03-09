package net.openid.conformance.security;

import com.google.common.collect.ImmutableMap;
import net.openid.conformance.sharing.magiclink.MagicLinkOneTimeToken;
import org.springframework.security.core.Authentication;

public interface AuthenticationFacade {

	boolean isAdmin();

	boolean isUser();

	boolean isMagicLinkUser();

	MagicLinkOneTimeToken getMagicOneTimeToken();

	ImmutableMap<String, String> getPrincipal();

	String getDisplayName();

	void setLocalAuthentication(Authentication a);

	Authentication getContextAuthentication();

}
