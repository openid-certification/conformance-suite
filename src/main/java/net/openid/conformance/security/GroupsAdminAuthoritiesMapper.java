package net.openid.conformance.security;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import net.openid.conformance.support.mitre.compat.oidc.UserInfo;
import net.openid.conformance.support.mitre.compat.spring.OIDCAuthoritiesMapper;
import net.openid.conformance.support.mitre.compat.spring.SubjectIssuerGrantedAuthority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;

import java.text.ParseException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Simple mapper that adds ROLE_USER to the authorities map for all queries,
 * plus adds ROLE_ADMIN if the userInfo or id_token 'groups' member (provided by gitlab or Azure) contains a specific group name.
 */
public class GroupsAdminAuthoritiesMapper implements OIDCAuthoritiesMapper {
	private static final Logger logger = LoggerFactory.getLogger(GroupsAdminAuthoritiesMapper.class);

	private String ADMIN_GROUP;

	private String ADMIN_ISSUER;

	@Override
	public Collection<? extends GrantedAuthority> mapAuthorities(JWT idToken, UserInfo userInfo) {

		Set<GrantedAuthority> out = new HashSet<>();

		try {
			JWTClaimsSet claims = idToken.getJWTClaimsSet();
			SubjectIssuerGrantedAuthority authority = new SubjectIssuerGrantedAuthority(claims.getSubject(), claims.getIssuer());
			out.add(authority);
			if (claims.getIssuer().equalsIgnoreCase(ADMIN_ISSUER))
			{
				JsonElement groupsEl = null;
				if (userInfo != null) {
					groupsEl = userInfo.getSource().get("groups");
				}
				if (groupsEl == null) {
					// if not in userinfo, check in id_token instead (azure can only put it here)
					JsonObject idTokenClaims = JsonParser.parseString(claims.toString()).getAsJsonObject();
					groupsEl = idTokenClaims.get("groups");
				}
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

	public GroupsAdminAuthoritiesMapper(String adminGroup, String adminIss) {

		this.ADMIN_GROUP = adminGroup;
		this.ADMIN_ISSUER = adminIss;
	}
}
