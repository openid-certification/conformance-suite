package net.openid.conformance.support.mitre.compat.spring;

import com.nimbusds.jwt.JWT;
import net.openid.conformance.support.mitre.compat.oidc.UserInfo;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public interface OIDCAuthoritiesMapper {

	/**
	 * @param idToken  the ID Token (parsed as a JWT, cannot be @null)
	 * @param userInfo userInfo of the current user (could be @null)
	 * @return the set of authorities to map to this user
	 */
	Collection<? extends GrantedAuthority> mapAuthorities(JWT idToken, UserInfo userInfo);

}
