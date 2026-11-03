package net.openid.conformance.security;

import net.openid.conformance.security.idp.RolesAuthoritiesConverter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
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

	private static Set<String> roleAuthorities(String issuer) {
		OidcIdToken idToken = OidcIdToken.withTokenValue("id-token")
			.claim("iss", issuer)
			.claim("sub", "a-user")
			.claim("roles", List.of("conformance-admin"))
			.build();
		return WebSecurityOidcLoginConfig.idpRoleAuthorities(new OidcUserAuthority(idToken), IDP_ISS,
				new RolesAuthoritiesConverter("conformance-admin"))
			.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
	}

	@Test
	public void the_idp_roles_claim_grants_admin() {
		Assertions.assertTrue(roleAuthorities(IDP_ISS).contains("ROLE_ADMIN"));
	}

	@Test
	public void another_providers_roles_claim_grants_nothing() {
		Assertions.assertEquals(Set.of(), roleAuthorities("https://gitlab.com"));
	}
}
