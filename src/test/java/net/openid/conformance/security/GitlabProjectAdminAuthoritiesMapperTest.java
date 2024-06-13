package net.openid.conformance.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class GitlabProjectAdminAuthoritiesMapperTest {

	@Test
	public void shouldGrantAdminRoleForGitlabProjectRole() {

		var idToken = OidcIdToken.withTokenValue("dummy").issuer("https://gitlab.com").build();
		var userInfo = OidcUserInfo.builder()
			.claim("https://gitlab.org/claims/groups/maintainer", List.of("oidc"))
			.build();

		var gitlabAdminGroupIndicatorClaims = Map.of("https://gitlab.org/claims/groups/maintainer", Set.of("oidc"));
		var mapper = new GitlabProjectAdminAuthoritiesMapper("https://gitlab.com", gitlabAdminGroupIndicatorClaims);

		var grantedAuthorities = new HashSet<GrantedAuthority>(mapper.mapAuthorities(idToken, userInfo));

		assertThat(grantedAuthorities).contains(OIDCAuthenticationFacade.ROLE_ADMIN);
	}

	@Test
	public void shouldGrantAdminRoleForGitlabProjectRoleWithMultipleRoles() {

		var idToken = OidcIdToken.withTokenValue("dummy").issuer("https://gitlab.com").build();
		var userInfo = OidcUserInfo.builder()
			.claim("https://gitlab.org/claims/groups/maintainer", List.of("foo", "bar"))
			.claim("https://gitlab.org/claims/groups/owner", List.of("oidc"))
			.build();

		var gitlabAdminGroupIndicatorClaims = Map.of(
			"https://gitlab.org/claims/groups/maintainer", Set.of("oidc"),
			"https://gitlab.org/claims/groups/owner", Set.of("oidc"));
		var mapper = new GitlabProjectAdminAuthoritiesMapper("https://gitlab.com", gitlabAdminGroupIndicatorClaims);

		var grantedAuthorities = new HashSet<GrantedAuthority>(mapper.mapAuthorities(idToken, userInfo));

		assertThat(grantedAuthorities).contains(OIDCAuthenticationFacade.ROLE_ADMIN);
	}

	@Test
	public void shouldNotGrantAdminRoleForGitlabProjectRole() {

		var idToken = OidcIdToken.withTokenValue("dummy").issuer("https://gitlab.com").build();
		var userInfo = OidcUserInfo.builder()
			.claim("https://gitlab.org/claims/groups/developer", List.of("oidc"))
			.build();

		var gitlabAdminGroupIndicatorClaims = Map.of("https://gitlab.org/claims/groups/maintainer", Set.of("oidc"));
		var mapper = new GitlabProjectAdminAuthoritiesMapper("https://gitlab.com", gitlabAdminGroupIndicatorClaims);

		var grantedAuthorities = new HashSet<GrantedAuthority>(mapper.mapAuthorities(idToken, userInfo));

		assertThat(grantedAuthorities).doesNotContain(OIDCAuthenticationFacade.ROLE_ADMIN);
	}
}
