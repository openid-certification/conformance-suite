package net.openid.conformance.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * Simple mapper that adds ROLE_USER to the auhorities map for all queries,
 * plus adds ROLE_ADMIN if the userInfo contains specific 'hd' (Hosted Domains) from Google.
 *
 */
public class GoogleHostedDomainAdminAuthoritiesMapper implements OIDCAuthoritiesMapper {

	private String ADMIN_DOMAINS;

	private String ADMIN_ISSUER;

	@Override
	public Collection<? extends GrantedAuthority> mapAuthorities(OidcIdToken idToken, OidcUserInfo userInfo) {

		Set<GrantedAuthority> out = new HashSet<>();

		String subject = idToken.getSubject();
		String issuer = idToken.getIssuer().toString();
		SubjectIssuerGrantedAuthority authority = new SubjectIssuerGrantedAuthority(subject, issuer);
		out.add(authority);
		if (issuer.equalsIgnoreCase(ADMIN_ISSUER)
			&& userInfo.hasClaim("hd"))
		{
			String[] adminDomainArray = ADMIN_DOMAINS.split(",");

			for (String domain : adminDomainArray) {
				if (userInfo.getClaimAsString("hd").equals(domain)) {
					out.add(OIDCAuthenticationFacade.ROLE_ADMIN);
					break;
				}
			}
		}
		out.add(OIDCAuthenticationFacade.ROLE_USER);
		return out;
	}

	public GoogleHostedDomainAdminAuthoritiesMapper(String adminDomains, String adminIss) {

		this.ADMIN_DOMAINS = adminDomains;
		this.ADMIN_ISSUER = adminIss;
	}
}
