package net.openid.conformance.util;

import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
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

}
