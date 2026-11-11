package net.openid.conformance.security.idp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Grants ROLE_USER to every IdP-authenticated principal, and ROLE_ADMIN to those
 * whose "roles" claim contains the configured admin role. A blank admin role grants
 * nobody admin.
 * <p>
 * The claim is read through the untyped {@code getClaim} rather than
 * {@code Jwt#getClaimAsStringList}: an IdP is free to send "roles" as something
 * other than a JSON array, and that must grant no admin rather than fail the
 * request. The typed accessor throws IllegalArgumentException when the claim will
 * not convert, which is not an AuthenticationException and would surface as a 500
 * instead of a 401.
 */
@Component
public class RolesAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

	private final String adminRole;

	// Constructor injection, not a @Value field: a hand-constructed converter then
	// cannot silently end up with a null admin role and quietly grant nobody admin.
	public RolesAuthoritiesConverter(
		@Value("${oidc.idp.admin-role:}") String adminRole) {
		this.adminRole = adminRole;
	}

	@Override
	public Collection<GrantedAuthority> convert(Jwt jwt) {
		Set<GrantedAuthority> authorities = new HashSet<>();
		authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
		if (isAdmin(jwt.getClaim("roles"))) {
			authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
		}
		return authorities;
	}

	private boolean isAdmin(Object rolesClaim) {
		return adminRole != null && !adminRole.isBlank()
			&& rolesClaim instanceof Collection<?> roles
			&& roles.contains(adminRole);
	}
}
