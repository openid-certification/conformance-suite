package net.openid.conformance.security;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import net.openid.conformance.testmodule.OIDFJSON;
import org.mitre.openid.connect.client.OIDCAuthoritiesMapper;
import org.mitre.openid.connect.client.SubjectIssuerGrantedAuthority;
import org.mitre.openid.connect.model.UserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;

import java.text.ParseException;
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
	private static final Logger logger = LoggerFactory.getLogger(GoogleHostedDomainAdminAuthoritiesMapper.class);

	private String ADMIN_DOMAINS;

	private String ADMIN_ISSUER;

	@Override
	public Collection<? extends GrantedAuthority> mapAuthorities(JWT idToken, UserInfo userInfo) {

		Set<GrantedAuthority> out = new HashSet<>();

		try {
			JWTClaimsSet claims = idToken.getJWTClaimsSet();
			SubjectIssuerGrantedAuthority authority = new SubjectIssuerGrantedAuthority(claims.getSubject(), claims.getIssuer());
			out.add(authority);
			if (claims.getIssuer().equalsIgnoreCase(ADMIN_ISSUER)
				&& userInfo.getSource().has("hd"))
			{
				String[] adminDomainArray = ADMIN_DOMAINS.split(",");

				for (int i = 0; i < adminDomainArray.length; i++) {
					String domain = adminDomainArray[i];
					if (OIDFJSON.getString(userInfo.getSource().get("hd")).equals(domain)) {
						out.add(OIDCAuthenticationFacade.ROLE_ADMIN);
						break;
					}
				}
			}
			out.add(OIDCAuthenticationFacade.ROLE_USER);
		} catch (ParseException e) {
			logger.error("Unable to parse ID Token inside of authorities mapper", e);
		}
		return out;
	}

	public GoogleHostedDomainAdminAuthoritiesMapper(String adminDomains, String adminIss) {

		this.ADMIN_DOMAINS = adminDomains;
		this.ADMIN_ISSUER = adminIss;
	}
}
