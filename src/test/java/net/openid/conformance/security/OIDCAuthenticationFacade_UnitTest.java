package net.openid.conformance.security;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * The facade turns whichever Authentication the filter chain produced into the
 * owner record stored on tests and plans, and into the display name the UI shows.
 * Both have to work for every principal type the chain can produce — an OIDC
 * login, an API token (now a JwtAuthenticationToken), and a private link.
 */
public class OIDCAuthenticationFacade_UnitTest {

	private static final List<SimpleGrantedAuthority> AUTHORITIES =
		List.of(new SimpleGrantedAuthority("ROLE_USER"));

	private final OIDCAuthenticationFacade facade = new OIDCAuthenticationFacade();

	@AfterEach
	public void tearDown() {
		SecurityContextHolder.clearContext();
	}

	private void authenticateAs(Authentication authentication) {
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}

	private static JwtAuthenticationToken apiToken(Map<String, Object> claims) {
		Jwt jwt = new Jwt("the-token", Instant.now(), Instant.now().plusSeconds(60),
			Map.of("typ", "jwt"), claims);
		return new JwtAuthenticationToken(jwt, AUTHORITIES);
	}

	private static OAuth2AuthenticationToken oidcLogin(Map<String, Object> claims) {
		OidcIdToken idToken = OidcIdToken.withTokenValue("id-token").claims(c -> c.putAll(claims)).build();
		return new OAuth2AuthenticationToken(new DefaultOidcUser(AUTHORITIES, idToken), AUTHORITIES, "idp");
	}

	/**
	 * An API token's claims come from its database row: an owner, a token value and
	 * an expiry, and no human-readable name at all. Returning "" here blanks the
	 * displayName on /api/currentuser, which the UI shows as the signed-in user.
	 */
	@Test
	public void display_name_of_an_api_token_falls_back_to_the_subject() {
		authenticateAs(apiToken(Map.of("iss", "https://issuer.example.com", "sub", "security-test")));

		Assertions.assertEquals("security-test", facade.getDisplayName());
	}

	@Test
	public void display_name_of_a_bearer_token_prefers_email() {
		authenticateAs(apiToken(Map.of(
			"iss", "https://issuer.example.com", "sub", "a-user",
			"email", "user@example.com", "name", "A User")));

		Assertions.assertEquals("user@example.com", facade.getDisplayName());
	}

	@Test
	public void display_name_of_a_bearer_token_uses_name_when_there_is_no_email() {
		authenticateAs(apiToken(Map.of(
			"iss", "https://issuer.example.com", "sub", "a-user", "name", "A User")));

		Assertions.assertEquals("A User", facade.getDisplayName());
	}

	@Test
	public void display_name_of_an_oidc_login_prefers_email_then_name_then_subject() {
		authenticateAs(oidcLogin(Map.of("iss", "https://idp.example.com", "sub", "a-user",
			"email", "user@example.com", "name", "A User")));
		Assertions.assertEquals("user@example.com", facade.getDisplayName());

		authenticateAs(oidcLogin(Map.of("iss", "https://idp.example.com", "sub", "a-user", "name", "A User")));
		Assertions.assertEquals("A User", facade.getDisplayName());

		authenticateAs(oidcLogin(Map.of("iss", "https://idp.example.com", "sub", "a-user")));
		Assertions.assertEquals("a-user", facade.getDisplayName());
	}

	@Test
	public void principal_of_a_bearer_token_is_the_issuer_and_subject() {
		authenticateAs(apiToken(Map.of("iss", "https://issuer.example.com", "sub", "security-test")));

		Assertions.assertEquals(
			ImmutableMap.of("sub", "security-test", "iss", "https://issuer.example.com"),
			facade.getPrincipal());
	}

	@Test
	public void principal_of_an_oidc_login_is_the_issuer_and_subject() {
		authenticateAs(oidcLogin(Map.of("iss", "https://idp.example.com", "sub", "a-user")));

		Assertions.assertEquals(
			ImmutableMap.of("sub", "a-user", "iss", "https://idp.example.com"),
			facade.getPrincipal());
	}

	/**
	 * An OAuth2AuthenticationToken from a plain OAuth2 (non-OIDC) flow carries an
	 * OAuth2User, not an OidcUser. Casting it blindly would throw a
	 * ClassCastException out of every request that principal makes.
	 */
	@Test
	public void a_non_oidc_oauth2_principal_does_not_blow_up() {
		var principal = new DefaultOAuth2User(AUTHORITIES, Map.of("sub", "a-user"), "sub");
		var token = new OAuth2AuthenticationToken(principal, AUTHORITIES, "idp");

		authenticateAs(token);

		Assertions.assertNull(Assertions.assertDoesNotThrow(facade::getPrincipal));
		Assertions.assertEquals("", Assertions.assertDoesNotThrow(facade::getDisplayName));
	}

	@Test
	public void an_unknown_authentication_has_no_principal_and_no_display_name() {
		authenticateAs(new UsernamePasswordAuthenticationToken("someone", null, AUTHORITIES));

		Assertions.assertNull(facade.getPrincipal());
		Assertions.assertEquals("", facade.getDisplayName());
	}

	@Test
	public void an_unauthenticated_context_has_no_principal() {
		SecurityContextHolder.clearContext();

		Assertions.assertNull(facade.getPrincipal());
		Assertions.assertEquals("", facade.getDisplayName());
	}
}
