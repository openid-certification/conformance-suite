package net.openid.conformance.security;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;

import java.util.List;

/**
 * The algorithms used to verify IdP-issued tokens come from configuration. A typo
 * must fail loudly at startup rather than silently degrading to an algorithm the
 * IdP does not use, and "none" or an HMAC algorithm must never be accepted.
 */
public class IdpJwsAlgorithms_UnitTest {

	@Test
	public void parses_a_single_algorithm() {
		Assertions.assertEquals(List.of(SignatureAlgorithm.ES256), IdpJwsAlgorithms.parse("ES256"));
	}

	@Test
	public void parses_a_list_preserving_order_and_dropping_duplicates() {
		Assertions.assertEquals(
			List.of(SignatureAlgorithm.ES256, SignatureAlgorithm.RS256),
			IdpJwsAlgorithms.parse(" ES256 , RS256 ,ES256 "));
	}

	/**
	 * Keycloak signs access tokens with RS256 by default even when its ID tokens are
	 * ES256, so clearing the property must keep both.
	 */
	@Test
	public void blank_configuration_falls_back_to_es256_and_rs256() {
		List<SignatureAlgorithm> expected = List.of(SignatureAlgorithm.ES256, SignatureAlgorithm.RS256);
		Assertions.assertEquals(expected, IdpJwsAlgorithms.parse(""));
		Assertions.assertEquals(expected, IdpJwsAlgorithms.parse("   "));
		Assertions.assertEquals(expected, IdpJwsAlgorithms.parse(" , "));
		Assertions.assertEquals(expected, IdpJwsAlgorithms.parse(null));
	}

	/**
	 * "none" and the HS* MAC algorithms are not asymmetric signatures. Accepting
	 * either would let a token that was never signed by the IdP's key authenticate
	 * a user, so they must be rejected rather than quietly ignored.
	 */
	@Test
	public void rejects_none_and_mac_algorithms() {
		Assertions.assertThrows(IllegalStateException.class, () -> IdpJwsAlgorithms.parse("none"));
		Assertions.assertThrows(IllegalStateException.class, () -> IdpJwsAlgorithms.parse("HS256"));
		Assertions.assertThrows(IllegalStateException.class, () -> IdpJwsAlgorithms.parse("ES256,HS256"));
	}

	@Test
	public void rejects_an_unknown_algorithm_name_with_an_actionable_message() {
		IllegalStateException e = Assertions.assertThrows(IllegalStateException.class,
			() -> IdpJwsAlgorithms.parse("ES256,RS2566"));

		Assertions.assertTrue(e.getMessage().contains("RS2566"), e.getMessage());
		Assertions.assertTrue(e.getMessage().contains("RS256"), e.getMessage());
	}
}
