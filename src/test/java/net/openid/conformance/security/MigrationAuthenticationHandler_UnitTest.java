package net.openid.conformance.security;

import net.openid.conformance.info.TestInfoService;
import net.openid.conformance.info.TestPlanService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import java.util.List;
import java.util.Map;

/**
 * The handler hands legacy-owned tests and plans over to the identity the IdP now
 * issues, and then completes the login. The login must survive every path through
 * it: a principal that carries no ID token, a token missing the legacy claims, and
 * a database that is refusing writes.
 */
public class MigrationAuthenticationHandler_UnitTest {

	private static final String LEGACY_ISS = "https://accounts.google.com";
	private static final String LEGACY_SUB = "legacy-user-id";

	private TestPlanService testPlanService;
	private TestInfoService testInfoService;
	private MigrationAuthenticationHandler handler;
	private MockHttpServletRequest request;
	private MockHttpServletResponse response;

	@BeforeEach
	public void setUp() {
		testPlanService = Mockito.mock(TestPlanService.class);
		testInfoService = Mockito.mock(TestInfoService.class);
		handler = new MigrationAuthenticationHandler(testPlanService, testInfoService);
		request = new MockHttpServletRequest();
		response = new MockHttpServletResponse();
	}

	private Authentication oidcLoginWithClaims(Map<String, Object> extraClaims) {
		var claims = new java.util.HashMap<String, Object>(Map.of(
			"iss", "https://openid.example.com/auth",
			"sub", "current-user-id"));
		claims.putAll(extraClaims);

		OidcIdToken idToken = OidcIdToken.withTokenValue("the-id-token").claims(c -> c.putAll(claims)).build();
		var authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
		return new OAuth2AuthenticationToken(new DefaultOidcUser(authorities, idToken), authorities, "idp");
	}

	@Test
	public void migrates_ownership_when_the_legacy_identity_claims_are_present() throws Exception {
		Mockito.when(testPlanService.migrateOwnership(LEGACY_ISS, LEGACY_SUB)).thenReturn(2L);
		Mockito.when(testInfoService.migrateOwnership(LEGACY_ISS, LEGACY_SUB)).thenReturn(7L);

		handler.onAuthenticationSuccess(request, response,
			oidcLoginWithClaims(Map.of("idp_iss", LEGACY_ISS, "idp_sub", LEGACY_SUB)));

		Mockito.verify(testPlanService).migrateOwnership(LEGACY_ISS, LEGACY_SUB);
		Mockito.verify(testInfoService).migrateOwnership(LEGACY_ISS, LEGACY_SUB);
		Assertions.assertNotNull(response.getRedirectedUrl(), "login must still complete");
	}

	/**
	 * The gate has to be the claims the migration actually reads. Gating on a
	 * separate "idp" claim let a token carrying "idp" but only one of the pair
	 * through to a half-specified migration.
	 */
	@Test
	public void does_not_migrate_when_only_one_of_the_legacy_claims_is_present() throws Exception {
		handler.onAuthenticationSuccess(request, response,
			oidcLoginWithClaims(Map.of("idp", "google", "idp_iss", LEGACY_ISS)));

		Mockito.verifyNoInteractions(testPlanService, testInfoService);
		Assertions.assertNotNull(response.getRedirectedUrl(), "login must still complete");
	}

	@Test
	public void does_not_migrate_for_a_login_that_carries_no_legacy_identity() throws Exception {
		handler.onAuthenticationSuccess(request, response, oidcLoginWithClaims(Map.of()));

		Mockito.verifyNoInteractions(testPlanService, testInfoService);
		Assertions.assertNotNull(response.getRedirectedUrl(), "login must still complete");
	}

	/**
	 * A non-OidcUser principal used to reach an unchecked cast, which would have
	 * thrown a ClassCastException out of the success handler and failed the login.
	 */
	@Test
	public void tolerates_a_principal_that_is_not_an_oidc_user() throws Exception {
		Authentication auth = new UsernamePasswordAuthenticationToken(
			"a-plain-principal", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));

		handler.onAuthenticationSuccess(request, response, auth);

		Mockito.verifyNoInteractions(testPlanService, testInfoService);
		Assertions.assertNotNull(response.getRedirectedUrl(), "login must still complete");
	}

	@Test
	public void login_completes_even_if_the_migration_fails() throws Exception {
		Mockito.when(testPlanService.migrateOwnership(LEGACY_ISS, LEGACY_SUB))
			.thenThrow(new org.springframework.dao.DataAccessResourceFailureException("mongo is down"));

		handler.onAuthenticationSuccess(request, response,
			oidcLoginWithClaims(Map.of("idp_iss", LEGACY_ISS, "idp_sub", LEGACY_SUB)));

		Assertions.assertNotNull(response.getRedirectedUrl(),
			"a migration failure must not lock the user out");
	}
}
