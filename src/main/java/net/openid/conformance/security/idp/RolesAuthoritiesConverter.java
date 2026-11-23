package net.openid.conformance.security.idp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Grants ROLE_USER to every IdP-authenticated principal, and ROLE_ADMIN to those
 * whose "roles" claim contains the configured admin role. A blank admin role grants
 * nobody admin. Applied to the IdP's ID tokens on a browser login.
 * <p>
 * The claim is taken untyped: an IdP is free to send "roles" as something other
 * than a JSON array, and that must grant no admin rather than fail the login.
 */
@Component
public class RolesAuthoritiesConverter {

	public static final String ROLES_CLAIM = "roles";

	private final String adminRole;

	// Constructor injection, not a @Value field: a hand-constructed converter then
	// cannot silently end up with a null admin role and quietly grant nobody admin.
	public RolesAuthoritiesConverter(
		@Value("${oidc.idp.admin-role:}") String adminRole) {
		this.adminRole = adminRole;
	}

	/**
	 * @param rolesClaim the "roles" claim of an ID token the IdP issued; may be null
	 *                   or of any type
	 */
	public Collection<GrantedAuthority> fromRolesClaim(Object rolesClaim) {
		Set<GrantedAuthority> authorities = new HashSet<>();
		authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
		if (isAdmin(rolesClaim)) {
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
