package net.openid.conformance.security;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * An ID token from the IdP has the same issuer and signing keys as its access
 * tokens, and carries the user's roles. Only the token type tells them apart.
 */
public class IdpAccessTokenTypeValidator_UnitTest {

	private final IdpAccessTokenTypeValidator validator = new IdpAccessTokenTypeValidator();

	private static Jwt token(String headerType, String claimType) {
		Jwt.Builder builder = Jwt.withTokenValue("token").header("alg", "ES256").claim("sub", "a-user");
		if (headerType != null) {
			builder.header("typ", headerType);
		}
		if (claimType != null) {
			builder.claim("typ", claimType);
		}
		return builder.build();
	}

	private boolean accepts(String headerType, String claimType) {
		return !validator.validate(token(headerType, claimType)).hasErrors();
	}

	@Test
	public void accepts_an_rfc9068_typ_header() {
		Assertions.assertTrue(accepts("at+jwt", null));
		Assertions.assertTrue(accepts("AT+JWT", null));
		Assertions.assertTrue(accepts("application/at+jwt", null));
	}

	@Test
	public void accepts_a_keycloak_bearer_typ_claim_under_a_generic_header() {
		Assertions.assertTrue(accepts("JWT", "Bearer"));
		Assertions.assertTrue(accepts(null, "Bearer"));
		Assertions.assertTrue(accepts("jwt", "bearer"));
	}

	@Test
	public void accepts_a_plain_jwt_header_without_a_typ_claim() {
		Assertions.assertTrue(accepts("JWT", null));
		Assertions.assertTrue(accepts("jwt", null));
	}

	@Test
	public void rejects_an_id_token() {
		Assertions.assertFalse(accepts("JWT", "ID"));
		Assertions.assertFalse(accepts(null, null), "an OIDC ID token carries no type marking at all");
		Assertions.assertFalse(accepts("", null));
	}

	@Test
	public void rejects_other_keycloak_token_types() {
		Assertions.assertFalse(accepts("JWT", "Refresh"));
		Assertions.assertFalse(accepts("JWT", "Logout"));
	}

	/**
	 * A Bearer claim only counts under a generic header: a JWT whose header says it
	 * is some other kind of token is that kind of token.
	 */
	@Test
	public void rejects_a_bearer_claim_under_a_specific_header() {
		Assertions.assertFalse(accepts("logout+jwt", "Bearer"));
		Assertions.assertFalse(accepts("dpop+jwt", "Bearer"));
	}
}
