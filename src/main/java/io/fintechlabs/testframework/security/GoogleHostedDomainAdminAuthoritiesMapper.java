package io.fintechlabs.testframework.security;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import org.mitre.openid.connect.client.OIDCAuthoritiesMapper;
import org.mitre.openid.connect.client.SubjectIssuerGrantedAuthority;
import org.mitre.openid.connect.model.UserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.text.ParseException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * Simple mapper that adds ROLE_USER to the auhorities map for all queries,
 * plus adds ROLE_ADMIN if the userInfo contains a specific 'hd' (Hosted Domain) from Google.
 *
 */
public class GoogleHostedDomainAdminAuthoritiesMapper implements OIDCAuthoritiesMapper {
	private static Logger logger = LoggerFactory.getLogger(GoogleHostedDomainAdminAuthoritiesMapper.class);

	private static final SimpleGrantedAuthority ROLE_ADMIN = new SimpleGrantedAuthority("ROLE_ADMIN");
	private static final SimpleGrantedAuthority ROLE_USER = new SimpleGrantedAuthority("ROLE_USER");

	@Value("${oidc.admin.domain:visexcell.com}")
	private  String ADMIN_DOMAIN;

	@Value("${oidc.admin.issuer:https://accounts.google.com}")
	private  String ADMIN_ISSUER;

	@Override
	public Collection<? extends GrantedAuthority> mapAuthorities(JWT idToken, UserInfo userInfo) {

		Set<GrantedAuthority> out = new HashSet<>();

		try{
			JWTClaimsSet claims = idToken.getJWTClaimsSet();
			SubjectIssuerGrantedAuthority authority = new SubjectIssuerGrantedAuthority(claims.getSubject(), claims.getIssuer());
			out.add(authority);
			if (claims.getIssuer().equalsIgnoreCase(ADMIN_ISSUER)
					&& userInfo.getSource().has("hd")
					&& userInfo.getSource().getAsJsonPrimitive("hd").getAsString().equals(ADMIN_DOMAIN)){
				out.add(ROLE_ADMIN);
			}
			out.add(ROLE_USER);
		} catch (ParseException e) {
			logger.error("Unable to parse ID Token inside of authorities mapper");
		}
		return out;
	}
}
