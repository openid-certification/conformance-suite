package net.openid.conformance.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The ROLE_ADMIN is added, iif the issuer is gitlab and the userinfo contains an "admin group indicator claim" controlled via
 * {@link #gitlabAdminGroupIndicatorClaims}.
 */
public class GitlabProjectAdminAuthoritiesMapper implements OIDCAuthoritiesMapper {

	private final String adminIssuer;

	private final Map<String, Set<String>> gitlabAdminGroupIndicatorClaims;

	public GitlabProjectAdminAuthoritiesMapper(String adminIssuer, Map<String, Set<String>> gitlabAdminGroupIndicatorClaims) {
		this.adminIssuer = adminIssuer;
		this.gitlabAdminGroupIndicatorClaims = gitlabAdminGroupIndicatorClaims;
	}

	@Override
	public Collection<? extends GrantedAuthority> mapAuthorities(OidcIdToken idToken, OidcUserInfo userInfo) {

		String issuer = idToken.getIssuer().toString();
		if (!issuer.equalsIgnoreCase(adminIssuer)) {
			return Collections.emptySet();
		}

		if (userInfo == null || CollectionUtils.isEmpty(gitlabAdminGroupIndicatorClaims)) {
			return Collections.emptySet();
		}

		for (var claimEntry : gitlabAdminGroupIndicatorClaims.entrySet()) {
			String claimName = claimEntry.getKey();
			Set<String> allowedClaimValues = claimEntry.getValue();
			List<String> actualClaimValues = userInfo.getClaimAsStringList(claimName);
			if (!CollectionUtils.isEmpty(actualClaimValues) && !CollectionUtils.isEmpty(allowedClaimValues)
				// check if any allowed claim values are contained in the actual claim values
				&& !Collections.disjoint(actualClaimValues, allowedClaimValues)) {
				return Set.of(OIDCAuthenticationFacade.ROLE_ADMIN);
			}
		}

		return Collections.emptySet();
	}
}
