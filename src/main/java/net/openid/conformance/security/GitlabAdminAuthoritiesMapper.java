package net.openid.conformance.security;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
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
 * Simple mapper that adds ROLE_USER to the authorities map for all queries,
 * plus adds ROLE_ADMIN if the userInfo 'groups' member (provided by gitlab) contains a specific group name.
 */
public class GitlabAdminAuthoritiesMapper implements OIDCAuthoritiesMapper {
	private static final Logger logger = LoggerFactory.getLogger(GitlabAdminAuthoritiesMapper.class);

	private String ADMIN_GROUP;

	private String ADMIN_ISSUER;

	@Override
	public Collection<? extends GrantedAuthority> mapAuthorities(JWT idToken, UserInfo userInfo) {

		Set<GrantedAuthority> out = new HashSet<>();

		try {
			JWTClaimsSet claims = idToken.getJWTClaimsSet();
			SubjectIssuerGrantedAuthority authority = new SubjectIssuerGrantedAuthority(claims.getSubject(), claims.getIssuer());
			out.add(authority);
			if (claims.getIssuer().equalsIgnoreCase(ADMIN_ISSUER)
				&& userInfo.getSource().has("groups"))
			{
				JsonElement groupsEl = userInfo.getSource().get("groups");
				if (groupsEl.isJsonArray()) {
					JsonArray groups = (JsonArray) groupsEl;
					JsonElement adminGroup = new JsonPrimitive(ADMIN_GROUP);
					if (groups.contains(adminGroup)) {
						out.add(OIDCAuthenticationFacade.ROLE_ADMIN);
					}
				}
			}
			out.add(OIDCAuthenticationFacade.ROLE_USER);
		} catch (ParseException e) {
			logger.error("Unable to parse ID Token inside of authorities mapper", e);
		}
		return out;
	}

	public GitlabAdminAuthoritiesMapper(String admin_group, String admin_iss) {

		this.ADMIN_GROUP = admin_group;
		this.ADMIN_ISSUER = admin_iss;
	}
}
