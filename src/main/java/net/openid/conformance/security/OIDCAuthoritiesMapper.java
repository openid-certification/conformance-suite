package net.openid.conformance.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;

import java.util.Collection;

public interface OIDCAuthoritiesMapper {

	/**
	 * @param idToken  the ID Token (parsed as a JWT, cannot be @null)
	 * @param userInfo userInfo of the current user (could be @null)
	 * @return the set of authorities to map to this user
	 */
	Collection<? extends GrantedAuthority> mapAuthorities(OidcIdToken idToken, OidcUserInfo userInfo);

}
