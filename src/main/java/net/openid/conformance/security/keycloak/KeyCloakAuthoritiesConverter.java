package net.openid.conformance.security.keycloak;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class KeyCloakAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>>, GrantedAuthoritiesMapper {

	@Value("${spring.security.oauth2.client.registration.idp.client-id}")
	private String clientId;

	@Value("${spring.security.oauth2.client.registration.idp.admin-role}")
	private String adminRole;

	@Override
	public Collection<GrantedAuthority> convert(Jwt jwt) {
		Set<GrantedAuthority> authorities = new HashSet<>();
		authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
		if (isAdmin(jwt.getClaimAsMap("resource_access"))) {
			authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
		}
		return authorities;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private boolean isAdmin(Map<String, Object> resourceAccess) {
		if (resourceAccess != null && resourceAccess.containsKey(clientId)) {
			Map<String, Object> conformanceResourceAccess = (Map<String, Object>) resourceAccess.get(clientId);

			Collection roles = (Collection) conformanceResourceAccess.getOrDefault("roles", Collections.EMPTY_LIST);
			return roles.contains(adminRole);
		}
		return false;
	}

	@Override
	public Collection<? extends GrantedAuthority> mapAuthorities(Collection<? extends GrantedAuthority> authorities) {
		Set<GrantedAuthority> extendedAuthorities = new HashSet<>(authorities);
		extendedAuthorities.add(new SimpleGrantedAuthority("ROLE_USER"));

		authorities.forEach(authority -> {
			if (authority instanceof OidcUserAuthority oidcUserAuthority) {
				if (isAdmin(oidcUserAuthority.getUserInfo().getClaimAsMap("resource_access"))) {
					extendedAuthorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
				}
			}
		});

		return extendedAuthorities;
	}
}
