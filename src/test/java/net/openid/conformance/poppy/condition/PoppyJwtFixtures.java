package net.openid.conformance.poppy.condition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds delegations and Session JWTs with Nimbus, independently of the suite's producer conditions.
 */
final class PoppyJwtFixtures {

	static final String CLIENT_ID = "https://agent.example/agent.json";
	static final String ISSUER = "https://as.example";
	static final String DELEGATION_TYP = "poppy-delegation+jwt";
	static final String SESSION_TYP = "poppy-session+jwt";

	final ECKey agentKey;
	final ECKey delegatedKey;
	final RSAKey rsaKey;

	PoppyJwtFixtures() {
		try {
			agentKey = new ECKeyGenerator(Curve.P_256).keyID("k1").algorithm(JWSAlgorithm.ES256).generate();
			delegatedKey = new ECKeyGenerator(Curve.P_256).keyIDFromThumbprint(true).generate();
			rsaKey = new RSAKeyGenerator(2048).keyID("rsa1").generate();
		} catch (JOSEException e) {
			throw new IllegalStateException(e);
		}
	}

	JsonObject clientJwks() {
		JWKSet set = new JWKSet(List.of(agentKey.toPublicJWK(), rsaKey.toPublicJWK()));
		return JsonParser.parseString(set.toString()).getAsJsonObject();
	}

	Map<String, Object> delegationClaims() {
		long now = Instant.now().getEpochSecond();
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("iss", CLIENT_ID);
		m.put("cnf", Map.of("jwk", delegatedKey.toPublicJWK().toJSONObject()));
		m.put("iat", now);
		m.put("exp", now + 3600);
		m.put("jti", "dlg-jti-1");
		return m;
	}

	String delegation() {
		return delegation(delegationClaims());
	}

	String delegation(Map<String, Object> claims) {
		return sign(JWSAlgorithm.ES256, DELEGATION_TYP, "k1", null, claims, ec(agentKey));
	}

	Map<String, Object> sessionClaims(String accessToken) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("iss", CLIENT_ID);
		m.put("aud", ISSUER);
		m.put("sid", "sid1");
		m.put("sub", "usr_abcdefghij");
		m.put("htm", "GET");
		m.put("htu", "https://h:8443/test/a/root-well-known/api/me");
		m.put("iat", Instant.now().getEpochSecond());
		m.put("jti", "sess-jti-1");
		if (accessToken != null) {
			m.put("ath", ath(accessToken));
		}
		return m;
	}

	String session(Map<String, Object> claims) {
		return sign(JWSAlgorithm.ES256, SESSION_TYP, delegatedKey.getKeyID(), delegation(), claims, ec(delegatedKey));
	}

	String session(Map<String, Object> claims, String dlg) {
		return sign(JWSAlgorithm.ES256, SESSION_TYP, delegatedKey.getKeyID(), dlg, claims, ec(delegatedKey));
	}

	static String sign(JWSAlgorithm alg, String typ, String kid, String dlg, Map<String, Object> claims, JWSSigner signer) {
		JWSHeader.Builder h = new JWSHeader.Builder(alg).type(new JOSEObjectType(typ)).keyID(kid);
		if (dlg != null) {
			h.customParam("poppy_dlg", dlg);
		}
		JWSObject obj = new JWSObject(h.build(), new Payload(claims));
		try {
			obj.sign(signer);
		} catch (JOSEException e) {
			throw new IllegalStateException(e);
		}
		return obj.serialize();
	}

	static String rsaPs384Delegation(RSAKey key, Map<String, Object> claims) {
		return sign(JWSAlgorithm.PS384, DELEGATION_TYP, key.getKeyID(), null, claims, rsa(key));
	}

	static JWSSigner ec(ECKey key) {
		try {
			return new ECDSASigner(key);
		} catch (JOSEException e) {
			throw new IllegalStateException(e);
		}
	}

	static JWSSigner rsa(RSAKey key) {
		try {
			return new RSASSASigner(key);
		} catch (JOSEException e) {
			throw new IllegalStateException(e);
		}
	}

	static String ath(String token) {
		try {
			return Base64.getUrlEncoder().withoutPadding().encodeToString(
				MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.US_ASCII)));
		} catch (java.security.NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
