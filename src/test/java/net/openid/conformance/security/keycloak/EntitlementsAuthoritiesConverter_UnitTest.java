package net.openid.conformance.security.keycloak;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Admin rights hang entirely off the "entitlements" claim, so both entry points
 * — the bearer-token converter and the OIDC-login authorities mapper — have to
 * agree, and neither may fail the request when an IdP sends the claim in a shape
 * it did not expect.
 */
public class EntitlementsAuthoritiesConverter_UnitTest {

	private static final String ADMIN_ROLE = "conformance-admin";
	private static final SimpleGrantedAuthority ROLE_USER = new SimpleGrantedAuthority("ROLE_USER");
	private static final SimpleGrantedAuthority ROLE_ADMIN = new SimpleGrantedAuthority("ROLE_ADMIN");

	private final EntitlementsAuthoritiesConverter converter = new EntitlementsAuthoritiesConverter(ADMIN_ROLE);

	private static Jwt jwtWithClaim(Object entitlements) {
		Jwt.Builder builder = Jwt.withTokenValue("token")
			.header("typ", "jwt")
			.claim("sub", "a-user");
		if (entitlements != null) {
			builder.claim("entitlements", entitlements);
		}
		return builder.build();
	}

	private static OidcUserAuthority oidcAuthorityWithClaim(Object entitlements) {
		Map<String, Object> claims = entitlements == null
			? Map.of("sub", "a-user")
			: Map.of("sub", "a-user", "entitlements", entitlements);
		OidcIdToken idToken = OidcIdToken.withTokenValue("token")
			.claims(c -> c.putAll(claims))
			.build();
		return new OidcUserAuthority(idToken, new OidcUserInfo(claims));
	}

	// --- bearer token path -------------------------------------------------

	@Test
	public void convert_grants_user_but_not_admin_without_the_admin_entitlement() {
		Collection<GrantedAuthority> authorities = converter.convert(jwtWithClaim(Map.of("some-other-role", Map.of())));

		Assertions.assertTrue(authorities.contains(ROLE_USER));
		Assertions.assertFalse(authorities.contains(ROLE_ADMIN));
	}

	@Test
	public void convert_grants_admin_when_the_entitlement_is_present() {
		Collection<GrantedAuthority> authorities = converter.convert(jwtWithClaim(Map.of(ADMIN_ROLE, Map.of())));

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
	 * Jwt#getClaimAsMap throws IllegalArgumentException when the claim will not
	 * convert. That is not an AuthenticationException, so it escapes the provider
	 * as a 500 instead of a 401 — an IdP emitting a list must cost admin rights,
	 * not break the API.
	 */
	@Test
	public void convert_tolerates_an_entitlements_claim_that_is_not_an_object() {
		for (Object shape : List.of(List.of(ADMIN_ROLE), ADMIN_ROLE, 42, true)) {
			Collection<GrantedAuthority> authorities =
				Assertions.assertDoesNotThrow(() -> converter.convert(jwtWithClaim(shape)),
					"entitlements as " + shape.getClass().getSimpleName() + " must not fail the request");
			Assertions.assertTrue(authorities.contains(ROLE_USER));
			Assertions.assertFalse(authorities.contains(ROLE_ADMIN));
		}
	}

	// --- OIDC login path ---------------------------------------------------

	@Test
	public void mapAuthorities_grants_admin_when_the_entitlement_is_present() {
		Collection<? extends GrantedAuthority> authorities =
			converter.mapAuthorities(List.of(oidcAuthorityWithClaim(Map.of(ADMIN_ROLE, Map.of()))));

		Assertions.assertTrue(authorities.contains(ROLE_USER));
		Assertions.assertTrue(authorities.contains(ROLE_ADMIN));
	}

	@Test
	public void mapAuthorities_grants_user_but_not_admin_otherwise() {
		Collection<? extends GrantedAuthority> authorities =
			converter.mapAuthorities(List.of(oidcAuthorityWithClaim(Map.of("some-other-role", Map.of()))));

		Assertions.assertTrue(authorities.contains(ROLE_USER));
		Assertions.assertFalse(authorities.contains(ROLE_ADMIN));
	}

	@Test
	public void mapAuthorities_tolerates_an_entitlements_claim_that_is_not_an_object() {
		Collection<? extends GrantedAuthority> authorities = Assertions.assertDoesNotThrow(
			() -> converter.mapAuthorities(List.of(oidcAuthorityWithClaim(List.of(ADMIN_ROLE)))));

		Assertions.assertTrue(authorities.contains(ROLE_USER));
		Assertions.assertFalse(authorities.contains(ROLE_ADMIN));
	}

	@Test
	public void mapAuthorities_preserves_the_incoming_authorities() {
		SimpleGrantedAuthority incoming = new SimpleGrantedAuthority("SCOPE_openid");

		Collection<? extends GrantedAuthority> authorities = converter.mapAuthorities(List.of(incoming));

		Assertions.assertTrue(authorities.contains(incoming));
		Assertions.assertTrue(authorities.contains(ROLE_USER));
	}

	/**
	 * The admin role name comes from configuration. A blank one must not match
	 * every entitlement map, and must not hand out admin by accident.
	 */
	@Test
	public void a_null_admin_role_never_grants_admin() {
		var unconfigured = new EntitlementsAuthoritiesConverter(null);

		Collection<GrantedAuthority> authorities = unconfigured.convert(jwtWithClaim(Map.of(ADMIN_ROLE, Map.of())));

		Assertions.assertTrue(authorities.contains(ROLE_USER));
		Assertions.assertFalse(authorities.contains(ROLE_ADMIN));
	}
}
