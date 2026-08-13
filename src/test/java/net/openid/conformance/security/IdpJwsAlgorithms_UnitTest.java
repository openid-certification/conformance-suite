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

	@Test
	public void blank_configuration_falls_back_to_the_oidc_default() {
		Assertions.assertEquals(List.of(SignatureAlgorithm.RS256), IdpJwsAlgorithms.parse(""));
		Assertions.assertEquals(List.of(SignatureAlgorithm.RS256), IdpJwsAlgorithms.parse("   "));
		Assertions.assertEquals(List.of(SignatureAlgorithm.RS256), IdpJwsAlgorithms.parse(null));
		Assertions.assertEquals(SignatureAlgorithm.RS256, IdpJwsAlgorithms.DEFAULT);
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

	@Test
	public void parseSingle_rejects_more_than_one_algorithm() {
		Assertions.assertEquals(SignatureAlgorithm.ES256, IdpJwsAlgorithms.parseSingle("ES256"));

		Assertions.assertThrows(IllegalStateException.class, () -> IdpJwsAlgorithms.parseSingle("ES256,RS256"));
	}

	@Test
	public void advertised_check_matches_the_discovery_document() {
		Assertions.assertTrue(IdpJwsAlgorithms.isAdvertised(List.of("RS256", "ES256"), SignatureAlgorithm.ES256));
		Assertions.assertFalse(IdpJwsAlgorithms.isAdvertised(List.of("RS256"), SignatureAlgorithm.ES256));
	}

	/**
	 * An IdP that advertises nothing usable gives us nothing to contradict, so the
	 * configured value stands rather than producing a spurious warning.
	 */
	@Test
	public void advertised_check_defers_when_the_idp_advertises_nothing() {
		Assertions.assertTrue(IdpJwsAlgorithms.isAdvertised(null, SignatureAlgorithm.ES256));
		Assertions.assertTrue(IdpJwsAlgorithms.isAdvertised(List.of(), SignatureAlgorithm.ES256));
	}
}
