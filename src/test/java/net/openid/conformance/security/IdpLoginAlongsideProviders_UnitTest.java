package net.openid.conformance.security;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * While the IdP is offered alongside Google and GitLab, each provider has to be
 * treated on its own terms: its own ID token signing algorithm, and admin rights
 * from the IdP's "roles" claim only for tokens the IdP issued.
 */
public class IdpLoginAlongsideProviders_UnitTest {

	private static final String IDP_ISS = "https://openid.example.com/auth";

	private static ClientRegistration registration(String id, Map<String, Object> metadata) {
		return ClientRegistration.withRegistrationId(id)
			.clientId(id + "-client")
			.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
			.redirectUri("{baseUrl}/login/oauth2/code/" + id)
			.authorizationUri("https://" + id + ".example.invalid/authorize")
			.tokenUri("https://" + id + ".example.invalid/token")
			.providerConfigurationMetadata(metadata)
			.build();
	}

	@Test
	public void the_idp_registration_uses_the_configured_algorithm() {
		Assertions.assertEquals(SignatureAlgorithm.ES256,
			WebSecurityOidcLoginConfig.idTokenSigningAlgorithm(registration("idp", Map.of()), SignatureAlgorithm.ES256));
	}

	/**
	 * Google and GitLab sign their ID tokens RS256; verifying them with the IdP's
	 * ES256 would fail every one of their logins.
	 */
	@Test
	public void google_and_gitlab_keep_rs256() {
		for (String id : List.of("google", "gitlab")) {
			Assertions.assertEquals(SignatureAlgorithm.RS256,
				WebSecurityOidcLoginConfig.idTokenSigningAlgorithm(
					registration(id, Map.of("id_token_signing_alg_values_supported", List.of("RS256"))),
					SignatureAlgorithm.ES256),
				id);
		}
	}

	private static Set<String> roleAuthorities(String issuer, Object roles) {
		OidcIdToken idToken = OidcIdToken.withTokenValue("id-token")
			.claim("iss", issuer)
			.claim("sub", "a-user")
			.claim("roles", roles)
			.build();
		return WebSecurityOidcLoginConfig.idpRoleAuthorities(idToken, IDP_ISS, "conformance-admin")
			.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
	}

	@Test
	public void the_idp_roles_claim_grants_admin() {
		Assertions.assertEquals(Set.of("ROLE_USER", "ROLE_ADMIN"),
			roleAuthorities(IDP_ISS, List.of("conformance-admin")));
	}

	@Test
	public void an_idp_login_without_the_admin_role_is_a_user() {
		Assertions.assertEquals(Set.of("ROLE_USER"), roleAuthorities(IDP_ISS, List.of("something-else")));
	}

	/**
	 * A "roles" claim that is not an array grants no admin, and does not fail the login.
	 */
	@Test
	public void a_roles_claim_of_the_wrong_shape_grants_no_admin() {
		Assertions.assertEquals(Set.of("ROLE_USER"), roleAuthorities(IDP_ISS, "conformance-admin"));
	}

	@Test
	public void another_providers_roles_claim_grants_nothing() {
		Assertions.assertEquals(Set.of(), roleAuthorities("https://gitlab.com", List.of("conformance-admin")));
	}
}
