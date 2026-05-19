package net.openid.conformance.sharing;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.factories.DefaultJWSSignerFactory;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.security.KeyManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ott.OneTimeToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssetSharing_UnitTest {

	private static final String BASE_URL = "https://example.com";
	private static final String PLAN_ID = "plan123";
	private static final String TEST_ID = "test456";
	private static final Map<String, String> OWNER = Map.of("sub", "user1", "iss", "https://issuer.example.com");

	private AssetSharing assetSharing;
	private KeyManager keyManager;

	@BeforeEach
	void setUp() {
		keyManager = new KeyManager();
		ReflectionTestUtils.setField(keyManager, "signingKey", "");
		ReflectionTestUtils.setField(keyManager, "deprecatedSigningKey", "");
		ReflectionTestUtils.setField(keyManager, "privateLinkSigningKey", "");
		keyManager.initializeKeyManager();

		assetSharing = new AssetSharing();
		ReflectionTestUtils.setField(assetSharing, "keyManager", keyManager);
		ReflectionTestUtils.setField(assetSharing, "baseURL", BASE_URL);
		assetSharing.init();
	}

	// --- Happy path round-trip tests ---

	@Test
	void plan_share_round_trip_produces_correct_claims() {
		OneTimeToken token = assetSharing.generateSharingToken(PLAN_ID, null, OWNER, "7");
		Jwt decoded = assetSharing.decodeSharingToken(token.getTokenValue());

		assertEquals(PLAN_ID, decoded.getClaimAsString("ct_plan_id"));
		assertNull(decoded.getClaimAsString("ct_test_id"));
		assertEquals(BASE_URL + "/plan-detail.html?plan=" + PLAN_ID, decoded.getClaimAsString("ct_redirect_uri"));
		assertNotNull(decoded.getId());
		assertNotNull(decoded.getIssuedAt());
		assertNotNull(decoded.getExpiresAt());
	}

	@Test
	void test_share_round_trip_produces_correct_claims() {
		OneTimeToken token = assetSharing.generateSharingToken(PLAN_ID, TEST_ID, OWNER, "7");
		Jwt decoded = assetSharing.decodeSharingToken(token.getTokenValue());

		assertEquals(PLAN_ID, decoded.getClaimAsString("ct_plan_id"));
		assertEquals(TEST_ID, decoded.getClaimAsString("ct_test_id"));
		assertEquals(BASE_URL + "/log-detail.html?log=" + TEST_ID, decoded.getClaimAsString("ct_redirect_uri"));
	}

	@Test
	void round_trip_preserves_owner() {
		OneTimeToken token = assetSharing.generateSharingToken(PLAN_ID, null, OWNER, "7");
		Jwt decoded = assetSharing.decodeSharingToken(token.getTokenValue());

		@SuppressWarnings("unchecked")
		Map<String, String> decodedOwner = decoded.getClaim("ct_testplan_owner");
		assertEquals("user1", decodedOwner.get("sub"));
		assertEquals("https://issuer.example.com", decodedOwner.get("iss"));
	}

	@Test
	void round_trip_sets_audience_and_issuer_to_base_url() {
		OneTimeToken token = assetSharing.generateSharingToken(PLAN_ID, null, OWNER, "7");
		Jwt decoded = assetSharing.decodeSharingToken(token.getTokenValue());

		assertTrue(decoded.getAudience().contains(BASE_URL));
		assertEquals(BASE_URL, decoded.getClaim("iss"));
	}

	// --- Token validation failure tests ---

	@Test
	void expired_token_is_rejected() throws Exception {
		JWK jwk = keyManager.getPrivateLinkKey();
		JWSAlgorithm alg = new JWSAlgorithm(jwk.getAlgorithm().toString());

		JWTClaimsSet claims = new JWTClaimsSet.Builder()
			.jwtID("expired-token")
			.issueTime(Date.from(Instant.now().minus(Duration.ofDays(10))))
			.expirationTime(Date.from(Instant.now().minus(Duration.ofDays(1))))
			.audience(BASE_URL)
			.issuer(BASE_URL)
			.claim("ct_plan_id", PLAN_ID)
			.claim("ct_testplan_owner", OWNER)
			.claim("ct_redirect_uri", BASE_URL + "/plan-detail.html?plan=" + PLAN_ID)
			.build();

		SignedJWT jwt = new SignedJWT(
			new JWSHeader.Builder(alg).type(JOSEObjectType.JWT).keyID(jwk.getKeyID()).build(),
			claims);
		JWSSigner signer = new DefaultJWSSignerFactory().createJWSSigner(jwk, alg);
		jwt.sign(signer);

		BadCredentialsException ex = assertThrows(BadCredentialsException.class,
			() -> assetSharing.decodeSharingToken(jwt.serialize()));
		assertEquals("JWT has expired", ex.getMessage());
	}

	@Test
	void wrong_audience_is_rejected() {
		// Generate token with the normal baseURL
		OneTimeToken token = assetSharing.generateSharingToken(PLAN_ID, null, OWNER, "7");

		// Create a second AssetSharing with a different baseURL for decoding
		AssetSharing otherSharing = new AssetSharing();
		ReflectionTestUtils.setField(otherSharing, "keyManager", keyManager);
		ReflectionTestUtils.setField(otherSharing, "baseURL", "https://other-server.com");
		otherSharing.init();

		BadCredentialsException ex = assertThrows(BadCredentialsException.class,
			() -> otherSharing.decodeSharingToken(token.getTokenValue()));
		assertEquals("Invalid sharing token audience", ex.getMessage());
	}

	@Test
	void wrong_issuer_is_rejected() throws Exception {
		// Craft a JWT where audience matches but issuer does not,
		// so the audience check passes and the issuer check is actually exercised.
		JWK jwk = keyManager.getPrivateLinkKey();
		JWSAlgorithm alg = new JWSAlgorithm(jwk.getAlgorithm().toString());

		JWTClaimsSet claims = new JWTClaimsSet.Builder()
			.jwtID("wrong-issuer-token")
			.issueTime(Date.from(Instant.now()))
			.expirationTime(Date.from(Instant.now().plus(Duration.ofDays(7))))
			.audience(BASE_URL)
			.issuer("https://wrong-issuer.com")
			.claim("ct_plan_id", PLAN_ID)
			.claim("ct_testplan_owner", OWNER)
			.claim("ct_redirect_uri", BASE_URL + "/plan-detail.html?plan=" + PLAN_ID)
			.build();

		SignedJWT jwt = new SignedJWT(
			new JWSHeader.Builder(alg).type(JOSEObjectType.JWT).keyID(jwk.getKeyID()).build(),
			claims);
		JWSSigner signer = new DefaultJWSSignerFactory().createJWSSigner(jwk, alg);
		jwt.sign(signer);

		BadCredentialsException ex = assertThrows(BadCredentialsException.class,
			() -> assetSharing.decodeSharingToken(jwt.serialize()));
		assertEquals("Invalid sharing token issuer", ex.getMessage());
	}

	@Test
	void tampered_signature_is_rejected() {
		OneTimeToken token = assetSharing.generateSharingToken(PLAN_ID, null, OWNER, "7");
		String serialized = token.getTokenValue();

		// Flip a character in the signature (last part of the JWT)
		char[] chars = serialized.toCharArray();
		int lastDot = serialized.lastIndexOf('.');
		int pos = lastDot + 1;
		chars[pos] = (chars[pos] == 'A') ? 'B' : 'A';
		String tampered = new String(chars);

		assertThrows(BadCredentialsException.class,
			() -> assetSharing.decodeSharingToken(tampered));
	}

	@Test
	void malformed_token_is_rejected() {
		BadCredentialsException ex = assertThrows(BadCredentialsException.class,
			() -> assetSharing.decodeSharingToken("not-a-jwt"));
		assertEquals("Invalid sharing token JWS", ex.getMessage());
	}

	// --- Expiration parameter validation ---

	@Test
	void expiration_zero_is_rejected() {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
			() -> assetSharing.generateSharingToken(PLAN_ID, null, OWNER, "0"));
		assertEquals("Expiration must be at least 1 day", ex.getMessage());
	}

	@Test
	void negative_expiration_is_rejected() {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
			() -> assetSharing.generateSharingToken(PLAN_ID, null, OWNER, "-1"));
		assertEquals("Expiration must be at least 1 day", ex.getMessage());
	}

	@Test
	void non_numeric_expiration_is_rejected() {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
			() -> assetSharing.generateSharingToken(PLAN_ID, null, OWNER, "abc"));
		assertEquals("Invalid expiration value: abc", ex.getMessage());
	}

	// --- getSharedAssetFromSharingToken ---

	@Test
	void getSharedAssetFromSharingToken_round_trip_for_plan() {
		OneTimeToken token = assetSharing.generateSharingToken(PLAN_ID, null, OWNER, "7");
		SharedAsset asset = assetSharing.getSharedAssetFromSharingToken(token.getTokenValue());

		assertNotNull(asset);
		assertEquals(PLAN_ID, asset.getPlanId());
		assertNull(asset.getTestId());
		assertEquals("user1", asset.getOwner().get("sub"));
		assertEquals("https://issuer.example.com", asset.getOwner().get("iss"));
		assertEquals(BASE_URL + "/plan-detail.html?plan=" + PLAN_ID, asset.getRedirectUri());
		assertNotNull(asset.getTokenId());
	}

	@Test
	void getSharedAssetFromSharingToken_round_trip_for_test() {
		OneTimeToken token = assetSharing.generateSharingToken(PLAN_ID, TEST_ID, OWNER, "7");
		SharedAsset asset = assetSharing.getSharedAssetFromSharingToken(token.getTokenValue());

		assertNotNull(asset);
		assertEquals(PLAN_ID, asset.getPlanId());
		assertEquals(TEST_ID, asset.getTestId());
		assertEquals(BASE_URL + "/log-detail.html?log=" + TEST_ID, asset.getRedirectUri());
	}

	// --- generateShareLink ---

	@Test
	void generateShareLink_returns_link_token_and_message() {
		Map<String, String> result = assetSharing.generateShareLink(PLAN_ID, null, OWNER, "7");

		assertTrue(result.containsKey("link"));
		assertTrue(result.containsKey("token"));
		assertTrue(result.containsKey("message"));
		assertTrue(result.get("link").startsWith(BASE_URL + "/login.html?token="));
	}

	@Test
	void generateShareLink_token_matches_jwt_embedded_in_link() {
		Map<String, String> result = assetSharing.generateShareLink(PLAN_ID, null, OWNER, "7");

		String token = result.get("token");
		String link = result.get("link");
		assertEquals(BASE_URL + "/login.html?token=" + token, link);

		// The returned token is a usable share JWT on its own.
		SharedAsset asset = assetSharing.getSharedAssetFromSharingToken(token);
		assertNotNull(asset);
		assertEquals(PLAN_ID, asset.getPlanId());
	}

	@Test
	void generateShareLink_message_warns_when_key_not_configured() {
		// KeyManager with empty privateLinkSigningKey generates an ephemeral key
		// and privateLinkKeyWasConfigured() returns false
		Map<String, String> result = assetSharing.generateShareLink(PLAN_ID, null, OWNER, "7");

		assertTrue(result.get("message").contains("invalidated on a server restart"),
			"Expected warning about key not being configured, got: " + result.get("message"));
	}
}
