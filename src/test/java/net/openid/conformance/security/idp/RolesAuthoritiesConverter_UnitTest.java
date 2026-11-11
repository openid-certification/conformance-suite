package net.openid.conformance.security.idp;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Admin rights for IdP bearer tokens hang entirely off the "roles" claim, and an
 * IdP sending the claim in a shape the converter did not expect must not fail the
 * request.
 */
public class RolesAuthoritiesConverter_UnitTest {

	private static final String ADMIN_ROLE = "conformance-admin";
	private static final SimpleGrantedAuthority ROLE_USER = new SimpleGrantedAuthority("ROLE_USER");
	private static final SimpleGrantedAuthority ROLE_ADMIN = new SimpleGrantedAuthority("ROLE_ADMIN");

	private final RolesAuthoritiesConverter converter = new RolesAuthoritiesConverter(ADMIN_ROLE);

	private static Jwt jwtWithClaim(Object roles) {
		Jwt.Builder builder = Jwt.withTokenValue("token")
			.header("typ", "jwt")
			.claim("sub", "a-user");
		if (roles != null) {
			builder.claim("roles", roles);
		}
		return builder.build();
	}

	@Test
	public void convert_grants_user_but_not_admin_without_the_admin_role() {
		Collection<GrantedAuthority> authorities = converter.convert(jwtWithClaim(List.of("some-other-role")));

		Assertions.assertTrue(authorities.contains(ROLE_USER));
		Assertions.assertFalse(authorities.contains(ROLE_ADMIN));
	}

	@Test
	public void convert_grants_admin_when_the_role_is_present() {
		Collection<GrantedAuthority> authorities =
			converter.convert(jwtWithClaim(List.of("some-other-role", ADMIN_ROLE)));

		Assertions.assertTrue(authorities.contains(ROLE_USER));
		Assertions.assertTrue(authorities.contains(ROLE_ADMIN));
	}

	@Test
	public void convert_grants_user_when_the_claim_is_absent() {
		Collection<GrantedAuthority> authorities = converter.convert(jwtWithClaim(null));

		Assertions.assertTrue(authorities.contains(ROLE_USER));
		Assertions.assertFalse(authorities.contains(ROLE_ADMIN));
	}

	/**
	 * Jwt#getClaimAsStringList throws IllegalArgumentException when the claim will
	 * not convert. That is not an AuthenticationException, so it would escape the
	 * provider as a 500 instead of a 401 — an IdP emitting the claim in another
	 * shape must cost admin rights, not break the API. That includes the object
	 * shape this code previously read ("entitlements" as a map of role name to
	 * details), so a half-migrated IdP loses admin rather than 500s.
	 */
	@Test
	public void convert_tolerates_a_roles_claim_that_is_not_an_array() {
		List<Object> shapes = List.of(Map.of(ADMIN_ROLE, Map.of()), ADMIN_ROLE, 42, true);
		for (Object shape : shapes) {
			Collection<GrantedAuthority> authorities =
				Assertions.assertDoesNotThrow(() -> converter.convert(jwtWithClaim(shape)),
					"roles as " + shape.getClass().getSimpleName() + " must not fail the request");
			Assertions.assertTrue(authorities.contains(ROLE_USER));
			Assertions.assertFalse(authorities.contains(ROLE_ADMIN));
		}
	}

	/**
	 * The admin role name comes from configuration and has no default. Neither a
	 * null nor a blank one may match anything in the roles array.
	 */
	@Test
	public void a_null_admin_role_never_grants_admin() {
		var unconfigured = new RolesAuthoritiesConverter(null);

		Collection<GrantedAuthority> authorities = unconfigured.convert(jwtWithClaim(List.of(ADMIN_ROLE)));

		Assertions.assertTrue(authorities.contains(ROLE_USER));
		Assertions.assertFalse(authorities.contains(ROLE_ADMIN));
	}

	@Test
	public void a_blank_admin_role_never_grants_admin() {
		var unconfigured = new RolesAuthoritiesConverter("");

		Collection<GrantedAuthority> authorities = unconfigured.convert(jwtWithClaim(List.of("")));

		Assertions.assertTrue(authorities.contains(ROLE_USER));
		Assertions.assertFalse(authorities.contains(ROLE_ADMIN));
	}
}
