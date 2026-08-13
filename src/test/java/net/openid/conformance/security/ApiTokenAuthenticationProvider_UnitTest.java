package net.openid.conformance.security;

import net.openid.conformance.token.TokenService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * The provider hands back a {@link JwtAuthenticationToken}, which is authenticated
 * from construction and never re-checks expiry, so expiry has to be enforced by the
 * provider itself. These tests pin that contract: an API token past its "expires"
 * timestamp must not authenticate.
 */
public class ApiTokenAuthenticationProvider_UnitTest {

	private static final String TOKEN = "the-api-token";

	private Authentication authenticateWithExpiry(Long expiresEpochMillis) {
		TokenService tokenService = Mockito.mock(TokenService.class);
		Map<String, Object> tokenInfo = new java.util.HashMap<>();
		tokenInfo.put("owner", Map.of("iss", "https://issuer.example.com", "sub", "the-user"));
		if (expiresEpochMillis != null) {
			tokenInfo.put("expires", expiresEpochMillis);
		}
		Mockito.when(tokenService.findToken(TOKEN)).thenReturn(tokenInfo);

		return new ApiTokenAuthenticationProvider(tokenService)
			.authenticate(new BearerTokenAuthenticationToken(TOKEN));
	}

	@Test
	public void expired_api_token_is_rejected() {
		Instant expired = Instant.now().minus(1, ChronoUnit.HOURS);

		Assertions.assertNull(authenticateWithExpiry(expired.toEpochMilli()),
			"an API token whose 'expires' has passed must not authenticate");
	}

	@Test
	public void unexpired_api_token_authenticates_with_the_owner_as_subject() {
		Instant expires = Instant.now().plus(1, ChronoUnit.HOURS);

		Authentication auth = authenticateWithExpiry(expires.toEpochMilli());

		Assertions.assertInstanceOf(JwtAuthenticationToken.class, auth);
		Jwt jwt = (Jwt) auth.getPrincipal();
		Assertions.assertEquals("the-user", jwt.getSubject());
		Assertions.assertEquals("https://issuer.example.com", jwt.getIssuer().toString());
		Assertions.assertTrue(auth.getAuthorities().contains(OIDCAuthenticationFacade.ROLE_USER));
	}

	@Test
	public void api_token_without_an_expiry_authenticates() {
		Authentication auth = authenticateWithExpiry(null);

		Assertions.assertInstanceOf(JwtAuthenticationToken.class, auth);
		Assertions.assertNull(((Jwt) auth.getPrincipal()).getExpiresAt());
	}

	/**
	 * A token row whose owner is missing or half-populated cannot identify anyone.
	 * It must be rejected as unauthenticated, not dereferenced into a
	 * NullPointerException escaping the filter chain as a 500.
	 */
	@Test
	public void api_token_without_a_usable_owner_is_rejected() {
		for (Object owner : new Object[] {null, Map.of(), Map.of("iss", "https://issuer.example.com")}) {
			TokenService tokenService = Mockito.mock(TokenService.class);
			Map<String, Object> tokenInfo = new java.util.HashMap<>();
			if (owner != null) {
				tokenInfo.put("owner", owner);
			}
			Mockito.when(tokenService.findToken(TOKEN)).thenReturn(tokenInfo);

			Assertions.assertNull(
				new ApiTokenAuthenticationProvider(tokenService)
					.authenticate(new BearerTokenAuthenticationToken(TOKEN)),
				"owner=" + owner + " must not authenticate");
		}
	}

	@Test
	public void unknown_api_token_is_rejected() {
		TokenService tokenService = Mockito.mock(TokenService.class);
		Mockito.when(tokenService.findToken(Mockito.anyString())).thenReturn(null);

		Assertions.assertNull(new ApiTokenAuthenticationProvider(tokenService)
			.authenticate(new BearerTokenAuthenticationToken("no-such-token")));
	}
}
