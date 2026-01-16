package net.openid.conformance.security.keycloak;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class KeyCloakAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>>, GrantedAuthoritiesMapper {


	@Override
	public Collection<GrantedAuthority> convert(Jwt jwt) {
		Set<GrantedAuthority> authorities = new HashSet<>();
		authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
		List<String> roles = jwt.getClaimAsStringList("roles");
		if (roles != null && roles.contains("conformance_super_admin")){
			authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
		}
		return authorities;
	}

	@Override
	public Collection<? extends GrantedAuthority> mapAuthorities(Collection<? extends GrantedAuthority> authorities) {
		Set<GrantedAuthority> extendedAuthorities = new HashSet<>(authorities);
		extendedAuthorities.add(new SimpleGrantedAuthority("ROLE_USER"));

		authorities.forEach(authority -> {
			if (authority instanceof OidcUserAuthority oidcUserAuthority) {
				var roles = oidcUserAuthority.getUserInfo().getClaimAsStringList("roles");
				if (roles.contains("conformance_super_admin")){
					extendedAuthorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
				}
			}
		});

		return extendedAuthorities;
	}
}
