package net.openid.conformance.security.keycloak;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Grants ROLE_USER to every authenticated principal, and ROLE_ADMIN to those whose
 * "entitlements" claim contains the configured admin role.
 * <p>
 * Both entry points read the claim defensively: an IdP is free to send
 * "entitlements" as something other than a JSON object, and that must grant no
 * admin rather than fail the request. In particular {@code Jwt#getClaimAsMap}
 * throws IllegalArgumentException when the claim will not convert, which is not
 * an AuthenticationException and would surface as a 500 instead of a 401.
 */
@Component
public class EntitlementsAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>>, GrantedAuthoritiesMapper {

	private final String adminRole;

	// Constructor injection, not a @Value field: a hand-constructed converter then
	// cannot silently end up with a null admin role and quietly grant nobody admin.
	public EntitlementsAuthoritiesConverter(
		@Value("${oidc.idp.admin-role}") String adminRole) {
		this.adminRole = adminRole;
	}

	@Override
	public Collection<GrantedAuthority> convert(Jwt jwt) {
		Set<GrantedAuthority> authorities = baseAuthorities();
		Object entitlements = jwt.getClaim("entitlements");
		if (entitlements instanceof Map<?, ?> claim && isAdmin(claim)) {
			authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
		}
		return authorities;
	}

	@Override
	public Collection<? extends GrantedAuthority> mapAuthorities(Collection<? extends GrantedAuthority> authorities) {
		Set<GrantedAuthority> extendedAuthorities = new HashSet<>(authorities);
		extendedAuthorities.addAll(baseAuthorities());

		authorities.forEach(authority -> {
			if (authority instanceof OidcUserAuthority oidcUserAuthority
				&& oidcUserAuthority.getAttributes().get("entitlements") instanceof Map<?, ?> claim
				&& isAdmin(claim)) {
				extendedAuthorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
			}
		});

		return extendedAuthorities;
	}

	private Set<GrantedAuthority> baseAuthorities() {
		Set<GrantedAuthority> authorities = new HashSet<>();
		authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
		return authorities;
	}

	private boolean isAdmin(Map<?, ?> entitlements) {
		return adminRole != null && entitlements.containsKey(adminRole);
	}
}
