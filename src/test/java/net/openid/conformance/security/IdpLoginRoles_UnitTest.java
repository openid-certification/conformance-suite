package net.openid.conformance.security;

import net.openid.conformance.security.idp.RolesAuthoritiesConverter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

/**
 * A browser login takes its roles from the ID token's "roles" claim only when the
 * oidc.idp.* provider issued that ID token.
 */
public class IdpLoginRoles_UnitTest {

	private static final String IDP = "https://idp.example.com";

	private static final String ADMIN_ROLE = "conformance-admin";

	private final RolesAuthoritiesConverter converter = new RolesAuthoritiesConverter(ADMIN_ROLE);

	private static OidcIdToken idToken(String issuer, Object roles) {
		OidcIdToken.Builder builder = OidcIdToken.withTokenValue("token")
			.issuer(issuer)
			.subject("a-user")
			.issuedAt(Instant.now())
			.expiresAt(Instant.now().plusSeconds(60));
		if (roles != null) {
			builder.claim("roles", roles);
		}
		return builder.build();
	}

	@Test
	public void an_idp_id_token_with_the_admin_role_grants_admin() {
		Collection<GrantedAuthority> authorities = WebSecurityOidcLoginConfig.idpRoleAuthorities(
			idToken(IDP, List.of("other", ADMIN_ROLE)), IDP, converter);

		Assertions.assertTrue(authorities.contains(OIDCAuthenticationFacade.ROLE_ADMIN));
		Assertions.assertTrue(authorities.contains(OIDCAuthenticationFacade.ROLE_USER));
	}

	@Test
	public void an_idp_id_token_without_the_admin_role_grants_only_user() {
		Assertions.assertEquals(List.of(OIDCAuthenticationFacade.ROLE_USER), List.copyOf(
			WebSecurityOidcLoginConfig.idpRoleAuthorities(idToken(IDP, List.of("other")), IDP, converter)));
		Assertions.assertEquals(List.of(OIDCAuthenticationFacade.ROLE_USER), List.copyOf(
			WebSecurityOidcLoginConfig.idpRoleAuthorities(idToken(IDP, null), IDP, converter)));
	}

	@Test
	public void another_providers_roles_claim_grants_nothing() {
		OidcIdToken fromElsewhere = idToken("https://gitlab.com", List.of(ADMIN_ROLE));

		Assertions.assertTrue(WebSecurityOidcLoginConfig.idpRoleAuthorities(fromElsewhere, IDP, converter).isEmpty());
	}

	@Test
	public void without_a_configured_idp_no_roles_claim_is_honoured() {
		OidcIdToken token = idToken(IDP, List.of(ADMIN_ROLE));

		Assertions.assertTrue(WebSecurityOidcLoginConfig.idpRoleAuthorities(token, "", converter).isEmpty());
		Assertions.assertTrue(WebSecurityOidcLoginConfig.idpRoleAuthorities(token, null, converter).isEmpty());
	}
}
