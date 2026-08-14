package net.openid.conformance.security;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Every access token the IdP issues carries the same issuer and is signed by the
 * same keys, so issuer plus signature does not distinguish a token minted for the
 * suite from one minted for any other client of the IdP — including a public
 * client a user can get a token from themselves. These tests pin the check that
 * keeps such a token from authenticating anyone here.
 */
public class IdpAudienceValidator_UnitTest {

	private static final String CLIENT_ID = "conformance-suite";

	private final IdpAudienceValidator validator = new IdpAudienceValidator(CLIENT_ID);

	private static Jwt jwt(List<String> audience, String azp) {
		Map<String, Object> claims = new HashMap<>();
		claims.put("iss", "https://idp.example.com");
		claims.put("sub", "a-user");
		if (audience != null) {
			claims.put("aud", audience);
		}
		if (azp != null) {
			claims.put("azp", azp);
		}
		return new Jwt("a-token", Instant.now(), Instant.now().plusSeconds(60),
			Map.of("alg", "RS256"), claims);
	}

	@Test
	public void a_token_whose_audience_names_this_client_is_accepted() {
		Assertions.assertFalse(validator.validate(jwt(List.of(CLIENT_ID), null)).hasErrors());
	}

	@Test
	public void a_token_audienced_to_several_clients_including_this_one_is_accepted() {
		Assertions.assertFalse(validator.validate(jwt(List.of("account", CLIENT_ID), null)).hasErrors());
	}

	/**
	 * Keycloak does not put the client id in aud by default — a stock realm issues
	 * access tokens audienced to "account" with the client in azp. Rejecting those
	 * would lock out every API caller of a realm that has no audience mapper.
	 */
	@Test
	public void a_token_whose_authorized_party_is_this_client_is_accepted() {
		Assertions.assertFalse(validator.validate(jwt(List.of("account"), CLIENT_ID)).hasErrors());
	}

	/**
	 * The confused-deputy case this validator exists for: a token the IdP minted
	 * for some other client, which is otherwise entirely valid.
	 */
	@Test
	public void a_token_issued_to_another_client_is_rejected() {
		OAuth2TokenValidatorResult result = validator.validate(jwt(List.of("some-other-app"), "some-other-app"));

		Assertions.assertTrue(result.hasErrors());
		Assertions.assertEquals("invalid_token", result.getErrors().iterator().next().getErrorCode(),
			"the error code drives the WWW-Authenticate detail on the 401");
	}

	@Test
	public void a_token_with_neither_claim_is_rejected() {
		Assertions.assertTrue(validator.validate(jwt(null, null)).hasErrors());
	}

	/**
	 * A missing client id must fail closed. Matching a null/blank configured value
	 * against a missing claim would silently accept everything — the exact failure
	 * this class was added to prevent, but invisible.
	 */
	@Test
	public void an_unconfigured_client_id_accepts_nothing() {
		Assertions.assertTrue(new IdpAudienceValidator(null).validate(jwt(List.of(CLIENT_ID), CLIENT_ID)).hasErrors());
		Assertions.assertTrue(new IdpAudienceValidator("  ").validate(jwt(null, null)).hasErrors());
	}
}
