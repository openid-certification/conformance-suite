package net.openid.conformance.sharing.privatelink;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.factories.DefaultJWSSignerFactory;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.security.KeyManager;
import net.openid.conformance.security.OIDCAuthenticationFacade;
import net.openid.conformance.sharing.AssetSharing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.ott.OneTimeToken;
import org.springframework.security.authentication.ott.OneTimeTokenAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShareJwtBearerAuthenticationProvider_UnitTest {

	private static final String BASE_URL = "https://example.com";
	private static final String PLAN_ID = "plan123";
	private static final String TEST_ID = "test456";
	private static final Map<String, String> OWNER = Map.of("sub", "user1", "iss", "https://issuer.example.com");

	private AssetSharing assetSharing;
	private KeyManager keyManager;
	private ShareJwtBearerAuthenticationProvider provider;

	@BeforeEach
	void setUp() {
		keyManager = new KeyManager();
		ReflectionTestUtils.setField(keyManager, "signingKey", "");
		ReflectionTestUtils.setField(keyManager, "privateLinkSigningKey", "");
		keyManager.initializeKeyManager();

		assetSharing = new AssetSharing();
		ReflectionTestUtils.setField(assetSharing, "keyManager", keyManager);
		ReflectionTestUtils.setField(assetSharing, "baseURL", BASE_URL);
		assetSharing.init();

		provider = new ShareJwtBearerAuthenticationProvider(assetSharing, new PrivateLinkUserDetailsService());
	}

	private Authentication bearer(String token) {
		return new BearerTokenAuthenticationToken(token);
	}

	@Test
	void supports_bearer_token_authentication_token() {
		assertTrue(provider.supports(BearerTokenAuthenticationToken.class));
	}

	@Test
	void valid_plan_jwt_returns_authenticated_one_time_token() {
		OneTimeToken share = assetSharing.generateSharingToken(PLAN_ID, null, OWNER, "7");

		Authentication result = provider.authenticate(bearer(share.getTokenValue()));

		assertNotNull(result);
		assertTrue(result.isAuthenticated());
		OneTimeTokenAuthenticationToken ott = assertInstanceOf(OneTimeTokenAuthenticationToken.class, result);

		assertTrue(ott.getAuthorities().contains(OIDCAuthenticationFacade.ROLE_PRIVATE_LINK_USER));
		assertTrue(ott.getAuthorities().contains(OIDCAuthenticationFacade.ROLE_USER));

		Object details = ott.getDetails();
		PrivateLinkOneTimeToken privateLink = assertInstanceOf(PrivateLinkOneTimeToken.class, details);
		assertEquals(share.getTokenValue(), privateLink.getTokenValue());
		assertNotNull(privateLink.getSharedAsset());
		assertEquals(PLAN_ID, privateLink.getSharedAsset().getPlanId());
		assertNull(privateLink.getSharedAsset().getTestId());
		assertTrue(privateLink.getUsername().startsWith("Guest "));
	}

	@Test
	void valid_test_jwt_carries_test_id_in_shared_asset() {
		OneTimeToken share = assetSharing.generateSharingToken(PLAN_ID, TEST_ID, OWNER, "7");

		Authentication result = provider.authenticate(bearer(share.getTokenValue()));

		OneTimeTokenAuthenticationToken ott = assertInstanceOf(OneTimeTokenAuthenticationToken.class, result);
		PrivateLinkOneTimeToken privateLink = (PrivateLinkOneTimeToken) ott.getDetails();
		assertEquals(PLAN_ID, privateLink.getSharedAsset().getPlanId());
		assertEquals(TEST_ID, privateLink.getSharedAsset().getTestId());
	}

	@Test
	void malformed_token_returns_null() {
		assertNull(provider.authenticate(bearer("not-a-jwt")));
	}

	@Test
	void opaque_looking_token_returns_null() {
		// Simulates an API token string reaching this provider first. The provider
		// must decline (return null) so the opaque-token provider can handle it.
		assertNull(provider.authenticate(bearer("abcdef1234567890")));
	}

	@Test
	void expired_jwt_returns_null() throws Exception {
		String expired = mintSignedJwt(Instant.now().minus(Duration.ofDays(10)),
			Instant.now().minus(Duration.ofDays(1)), BASE_URL, BASE_URL);

		assertNull(provider.authenticate(bearer(expired)));
	}

	@Test
	void wrong_audience_jwt_returns_null() throws Exception {
		String wrongAud = mintSignedJwt(Instant.now(), Instant.now().plus(Duration.ofDays(7)),
			"https://other-server.com", BASE_URL);

		assertNull(provider.authenticate(bearer(wrongAud)));
	}

	@Test
	void wrong_issuer_jwt_returns_null() throws Exception {
		String wrongIss = mintSignedJwt(Instant.now(), Instant.now().plus(Duration.ofDays(7)),
			BASE_URL, "https://wrong-issuer.com");

		assertNull(provider.authenticate(bearer(wrongIss)));
	}

	@Test
	void tampered_signature_returns_null() {
		OneTimeToken share = assetSharing.generateSharingToken(PLAN_ID, null, OWNER, "7");
		String serialized = share.getTokenValue();
		char[] chars = serialized.toCharArray();
		int pos = serialized.lastIndexOf('.') + 1;
		chars[pos] = (chars[pos] == 'A') ? 'B' : 'A';

		assertNull(provider.authenticate(bearer(new String(chars))));
	}

	private String mintSignedJwt(Instant iat, Instant exp, String aud, String iss) throws Exception {
		JWK jwk = keyManager.getPrivateLinkKey();
		JWSAlgorithm alg = new JWSAlgorithm(jwk.getAlgorithm().toString());
		JWTClaimsSet claims = new JWTClaimsSet.Builder()
			.jwtID("test-token")
			.issueTime(Date.from(iat))
			.expirationTime(Date.from(exp))
			.audience(aud)
			.issuer(iss)
			.claim("ct_plan_id", PLAN_ID)
			.claim("ct_testplan_owner", OWNER)
			.claim("ct_redirect_uri", BASE_URL + "/plan-detail.html?plan=" + PLAN_ID)
			.build();
		SignedJWT jwt = new SignedJWT(
			new JWSHeader.Builder(alg).type(JOSEObjectType.JWT).keyID(jwk.getKeyID()).build(),
			claims);
		JWSSigner signer = new DefaultJWSSignerFactory().createJWSSigner(jwk, alg);
		jwt.sign(signer);
		return jwt.serialize();
	}
}
