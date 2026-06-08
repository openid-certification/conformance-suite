package net.openid.conformance.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import net.openid.conformance.util.JWKUtil.JwkIssue;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JWKUtil_UnitTest {

	// A Brainpool key (BP-512) the JOSE library cannot handle, followed by a standard P-256 key.
	private static final String MIXED_JWKS = """
		{
		  "keys": [
		    {
		      "crv": "BP-512",
		      "kid": "brainpool-key",
		      "kty": "EC",
		      "use": "enc",
		      "x": "qK-xJ65UZAZh6TCXhNJmVCqxqeB9FhQjraNj5c1I94_tITCucpOzhg5zj30j8AsclZNuogcpg-7VOXymsYTb6w",
		      "y": "KwUNahuNHNm6YQldhyhWoi3KAJC-PRzXmyh86mH7k6vAZn1Wz7XliRMOADXXnzx0cyjc06Ck2JItCUUVxDV__A",
		      "alg": "ECDH-ES"
		    },
		    {
		      "crv": "P-256",
		      "kid": "p256-key",
		      "kty": "EC",
		      "use": "enc",
		      "x": "hydCmuqjOVEFWuItZQuuZ74KGChEKb9qg_D0uvJCsEM",
		      "y": "g73kkv_85yvsOtTrkA5kg2as03d5HsxlqCkbPEpAYz4",
		      "alg": "ECDH-ES"
		    }
		  ]
		}""";

	@Test
	public void parseJWKSetLeniently_skipsUnsupportedCurveAndKeepsUsableKey() throws ParseException {
		List<JWKUtil.SkippedJwk> skipped = new ArrayList<>();
		JWKSet jwks = JWKUtil.parseJWKSetLeniently(MIXED_JWKS, skipped);

		assertEquals(1, jwks.getKeys().size());
		ECKey key = (ECKey) jwks.getKeys().get(0);
		assertEquals("p256-key", key.getKeyID());
		assertEquals(Curve.P_256, key.getCurve());

		assertEquals(1, skipped.size());
		assertTrue(skipped.get(0).keyJson().toString().contains("brainpool-key"));
		assertNotNull(skipped.get(0).reason());
	}

	@Test
	public void parseJWKSetLeniently_returnsEmptySetWhenAllKeysUnsupported() throws ParseException {
		String onlyBrainpool = """
			{
			  "keys": [
			    {
			      "crv": "BP-512",
			      "kid": "brainpool-key",
			      "kty": "EC",
			      "use": "enc",
			      "x": "qK-xJ65UZAZh6TCXhNJmVCqxqeB9FhQjraNj5c1I94_tITCucpOzhg5zj30j8AsclZNuogcpg-7VOXymsYTb6w",
			      "y": "KwUNahuNHNm6YQldhyhWoi3KAJC-PRzXmyh86mH7k6vAZn1Wz7XliRMOADXXnzx0cyjc06Ck2JItCUUVxDV__A",
			      "alg": "ECDH-ES"
			    }
			  ]
			}""";

		List<JWKUtil.SkippedJwk> skipped = new ArrayList<>();
		JWKSet jwks = JWKUtil.parseJWKSetLeniently(onlyBrainpool, skipped);

		assertTrue(jwks.getKeys().isEmpty());
		assertEquals(1, skipped.size());
	}

	@Test
	public void parseJWKSetLeniently_throwsWhenKeysArrayMissing() {
		assertThrows(ParseException.class, () -> JWKUtil.parseJWKSetLeniently("{}", new ArrayList<>()));
	}

	// Locks in the property the "ignore unusable key" conformance tests rely on: the synthetic
	// unusable keys they advertise (a post-quantum-shaped AKP key with a non-existent parameter set,
	// and a made-up key type) are not parseable by the JOSE library and are therefore skipped, while
	// the real key is kept.
	@Test
	public void parseJWKSetLeniently_skipsPostQuantumAndUnknownKtyKeys() throws ParseException {
		String mixed = """
			{
			  "keys": [
			    {
			      "kty": "AKP",
			      "alg": "ML-KEM-9999",
			      "kid": "unusable-pq-enc-key",
			      "use": "enc",
			      "pub": "Z0FOY29uZm9ybWFuY2UtdGVzdC1wbGFjZWhvbGRlci1wdWJsaWMta2V5"
			    },
			    {
			      "kty": "OIDF-CONFORMANCE-UNSUPPORTED",
			      "kid": "unusable-unknown-enc-key",
			      "use": "enc"
			    },
			    {
			      "crv": "P-256",
			      "kid": "p256-key",
			      "kty": "EC",
			      "use": "enc",
			      "x": "hydCmuqjOVEFWuItZQuuZ74KGChEKb9qg_D0uvJCsEM",
			      "y": "g73kkv_85yvsOtTrkA5kg2as03d5HsxlqCkbPEpAYz4",
			      "alg": "ECDH-ES"
			    }
			  ]
			}""";

		List<JWKUtil.SkippedJwk> skipped = new ArrayList<>();
		JWKSet jwks = JWKUtil.parseJWKSetLeniently(mixed, skipped);

		assertEquals(1, jwks.getKeys().size());
		assertEquals("p256-key", jwks.getKeys().get(0).getKeyID());

		assertEquals(2, skipped.size());
		assertTrue(skipped.get(0).keyJson().toString().contains("unusable-pq-enc-key"));
		assertNotNull(skipped.get(0).reason());
	}

	private static JsonObject jwks(String json) {
		return JsonParser.parseString(json).getAsJsonObject();
	}

	private static final String P256_PUBLIC = """
		{ "kty": "EC", "crv": "P-256", "kid": "p256",
		  "x": "hydCmuqjOVEFWuItZQuuZ74KGChEKb9qg_D0uvJCsEM",
		  "y": "g73kkv_85yvsOtTrkA5kg2as03d5HsxlqCkbPEpAYz4", "alg": "ECDH-ES" }""";

	@Test
	public void findPrivateOrSymmetricKeyMembers_detectsPrivateKey() {
		List<JwkIssue> issues = JWKUtil.findPrivateOrSymmetricKeyMembers(jwks("""
			{ "keys": [ { "kty": "EC", "crv": "P-256", "kid": "priv",
			  "x": "hydCmuqjOVEFWuItZQuuZ74KGChEKb9qg_D0uvJCsEM",
			  "y": "g73kkv_85yvsOtTrkA5kg2as03d5HsxlqCkbPEpAYz4",
			  "d": "_ypRKTu_hyy29nTdJb4xKX9bu7-ArYC191aeBuVNooA" } ] }"""));
		assertEquals(1, issues.size());
		assertTrue(issues.get(0).detail().contains("'d'"));
	}

	// Regression for the original review finding: a private key on a curve the JOSE library cannot
	// parse (Brainpool) must still be detected, because the raw member scan does not depend on Nimbus
	// being able to parse the key.
	@Test
	public void findPrivateOrSymmetricKeyMembers_detectsPrivateKeyOnUnsupportedCurve() {
		List<JwkIssue> issues = JWKUtil.findPrivateOrSymmetricKeyMembers(jwks("""
			{ "keys": [ { "kty": "EC", "crv": "BP-512", "kid": "brainpool-priv",
			  "x": "qK-xJ65UZAZh6TCXhNJmVCqxqeB9FhQjraNj5c1I94_tITCucpOzhg5zj30j8AsclZNuogcpg-7VOXymsYTb6w",
			  "y": "KwUNahuNHNm6YQldhyhWoi3KAJC-PRzXmyh86mH7k6vAZn1Wz7XliRMOADXXnzx0cyjc06Ck2JItCUUVxDV__A",
			  "d": "anything" } ] }"""));
		assertEquals(1, issues.size());
		assertTrue(issues.get(0).detail().contains("private key material"));
	}

	@Test
	public void findPrivateOrSymmetricKeyMembers_detectsSymmetricKey() {
		List<JwkIssue> issues = JWKUtil.findPrivateOrSymmetricKeyMembers(jwks("""
			{ "keys": [ { "kty": "oct", "kid": "sym", "k": "c2VjcmV0" } ] }"""));
		assertEquals(1, issues.size());
		assertTrue(issues.get(0).detail().contains("symmetric"));
	}

	@Test
	public void findPrivateOrSymmetricKeyMembers_passesForPublicKeys() {
		// MIXED_JWKS holds a Brainpool public key and a P-256 public key - neither has private material.
		assertTrue(JWKUtil.findPrivateOrSymmetricKeyMembers(jwks(MIXED_JWKS)).isEmpty());
	}

	@Test
	public void findStructurallyInvalidKeys_detectsMissingMember() {
		List<JwkIssue> issues = JWKUtil.findStructurallyInvalidKeys(jwks("""
			{ "keys": [ { "kty": "RSA", "kid": "rsa", "e": "AQAB" } ] }"""));
		assertEquals(1, issues.size());
		assertTrue(issues.get(0).detail().contains("'n'"));
	}

	@Test
	public void findStructurallyInvalidKeys_detectsNonBase64Url() {
		List<JwkIssue> issues = JWKUtil.findStructurallyInvalidKeys(jwks("""
			{ "keys": [ { "kty": "EC", "crv": "P-256", "x": "not valid base64!", "y": "g73kkv_85yvsOtTrkA5kg2as03d5HsxlqCkbPEpAYz4" } ] }"""));
		assertEquals(1, issues.size());
		assertTrue(issues.get(0).detail().contains("base64url"));
	}

	@Test
	public void findStructurallyInvalidKeys_passesValidPublicKey() {
		assertTrue(JWKUtil.findStructurallyInvalidKeys(jwks("{ \"keys\": [ " + P256_PUBLIC + " ] }")).isEmpty());
	}

	@Test
	public void findStructurallyInvalidKeys_ignoresUnknownKeyType() {
		// An unknown kty is a warning (findUnusableKeys), not a structural failure.
		assertTrue(JWKUtil.findStructurallyInvalidKeys(jwks("""
			{ "keys": [ { "kty": "OIDF-CONFORMANCE-UNSUPPORTED", "kid": "x" } ] }""")).isEmpty());
	}

	@Test
	public void findUnusableKeys_flagsUnsupportedCurve() {
		List<JwkIssue> issues = JWKUtil.findUnusableKeys(jwks(MIXED_JWKS));
		assertEquals(1, issues.size());
		assertTrue(issues.get(0).detail().contains("unsupported EC curve"));
	}

	@Test
	public void findUnusableKeys_flagsUnknownKeyType() {
		List<JwkIssue> issues = JWKUtil.findUnusableKeys(jwks("""
			{ "keys": [ { "kty": "OIDF-CONFORMANCE-UNSUPPORTED", "kid": "x" } ] }"""));
		assertEquals(1, issues.size());
		assertTrue(issues.get(0).detail().contains("unsupported key type"));
	}

	@Test
	public void findUnusableKeys_flagsUnknownAlgorithm() {
		List<JwkIssue> issues = JWKUtil.findUnusableKeys(jwks("""
			{ "keys": [ { "kty": "EC", "crv": "P-256", "kid": "p256",
			  "x": "hydCmuqjOVEFWuItZQuuZ74KGChEKb9qg_D0uvJCsEM",
			  "y": "g73kkv_85yvsOtTrkA5kg2as03d5HsxlqCkbPEpAYz4", "alg": "ML-DSA-65" } ] }"""));
		assertEquals(1, issues.size());
		assertTrue(issues.get(0).detail().contains("unrecognised algorithm"));
	}

	@Test
	public void findUnusableKeys_passesStandardKey() {
		assertTrue(JWKUtil.findUnusableKeys(jwks("{ \"keys\": [ " + P256_PUBLIC + " ] }")).isEmpty());
	}

	@Test
	public void findUnparseableUsableKeys_passesValidKey() {
		assertTrue(JWKUtil.findUnparseableUsableKeys(jwks("{ \"keys\": [ " + P256_PUBLIC + " ] }")).isEmpty());
	}

	// A Brainpool key cannot be parsed by the JOSE library, but it is "unusable" (warned about
	// elsewhere) so it must NOT be reported as a parse failure here. The usable P-256 key parses.
	@Test
	public void findUnparseableUsableKeys_skipsUnusableKey() {
		assertTrue(JWKUtil.findUnparseableUsableKeys(jwks(MIXED_JWKS)).isEmpty());
	}

	@Test
	public void findUnparseableUsableKeys_flagsUsableKeyThatFailsToParse() {
		// kty/crv recognised (usable) and members present + base64url, but the EC coordinates are the
		// wrong length for P-256, so the JOSE library rejects the key.
		List<JwkIssue> issues = JWKUtil.findUnparseableUsableKeys(jwks("""
			{ "keys": [ { "kty": "EC", "crv": "P-256", "x": "AAAA", "y": "AAAA", "alg": "ES256" } ] }"""));
		assertEquals(1, issues.size());
		assertTrue(issues.get(0).detail().contains("JOSE library"));
	}

}
