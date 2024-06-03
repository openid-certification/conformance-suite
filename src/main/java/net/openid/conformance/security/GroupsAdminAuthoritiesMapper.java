package net.openid.conformance.security;

import net.openid.conformance.support.mitre.compat.spring.SubjectIssuerGrantedAuthority;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Simple mapper that adds ROLE_USER to the authorities map for all queries,
 * plus adds ROLE_ADMIN if the userInfo or id_token 'groups' member (provided by gitlab or Azure) contains a specific group name.
 */
public class GroupsAdminAuthoritiesMapper implements OIDCAuthoritiesMapper {

	private String ADMIN_GROUP;

	private String ADMIN_ISSUER;

	@Override
	public Collection<? extends GrantedAuthority> mapAuthorities(OidcIdToken idToken, OidcUserInfo userInfo) {

		Set<GrantedAuthority> out = new HashSet<>();

//		var claims = idToken.getClaims();
		String subject = idToken.getSubject();
		String issuer = idToken.getIssuer().toString();
		SubjectIssuerGrantedAuthority authority = new SubjectIssuerGrantedAuthority(subject, issuer);
		out.add(authority);
		if (issuer.equalsIgnoreCase(ADMIN_ISSUER))
		{
			List<String> groupsEl = null;
			if (userInfo != null) {
				groupsEl = userInfo.getClaim("groups");
			}
			if (groupsEl == null) {
				// if not in userinfo, check in id_token instead (azure can only put it here)
				groupsEl = idToken.getClaimAsStringList("groups");
			}
			if (groupsEl != null) {
				if (groupsEl.contains(ADMIN_GROUP)) {
					out.add(OIDCAuthenticationFacade.ROLE_ADMIN);
				}
			}
		}
		out.add(OIDCAuthenticationFacade.ROLE_USER);
		return out;
	}

	public GroupsAdminAuthoritiesMapper(String adminGroup, String adminIss) {

		this.ADMIN_GROUP = adminGroup;
		this.ADMIN_ISSUER = adminIss;
	}
}
