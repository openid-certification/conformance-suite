package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.jwk.gen.OctetSequenceKeyGenerator;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the {@code alg} handling in {@link AbstractVerifyJwsSignature#verifyJwsSignature}, the signature
 * verification shared by every protocol family (id_token, JARM, DPoP, entity statements, SETs, AuthZEN
 * signed_metadata, ...). Its callers are conditions in a dozen packages, so the guard is exercised here
 * through a minimal subclass rather than through any one of them — several callers validate {@code alg}
 * themselves before calling, and would never let a bad value reach this code.
 *
 * <p>The header {@code alg} decides which key can verify the signature, and Nimbus'
 * {@link com.nimbusds.jose.jwk.KeyType#forAlgorithm} is not a safe way to ask that question on its own:
 * it returns null for an unregistered name and maps the JWE key management algorithms onto a key type as
 * if they were signing algorithms.
 */
class AbstractVerifyJwsSignature_UnitTest {

	private static final String NOT_A_JWS_ALG = "not a registered JWS signature or MAC algorithm";

	private Harness cond;

	@BeforeEach
	public void setUp() {
		cond = new Harness();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
	}

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	/**
	 * A JWS-shaped token with an arbitrary {@code alg}. Built by hand because the Nimbus signing API only
	 * produces algorithms it supports, and the point here is the ones it does not.
	 */
	private static String tokenWithAlg(String alg) {
		String header = Base64URL.encode("{\"alg\":\"" + alg + "\"}").toString();
		String payload = Base64URL.encode("{\"iss\":\"https://example.com\"}").toString();
		return header + "." + payload + ".AAAA";
	}

	private static JsonObject jwkSet(com.nimbusds.jose.jwk.JWK key) {
		return JsonParser.parseString(new JWKSet(key).toString(false)).getAsJsonObject();
	}

	@Test
	public void unregisteredAlg_failsTheCondition() throws Exception {
		// KeyType.forAlgorithm() returns null here; that null used to be dereferenced, failing the test
		// with an internal NullPointerException instead of a condition failure.
		ECKey key = new ECKeyGenerator(Curve.P_256).generate();
		Throwable e = assertThrows(ConditionError.class,
			() -> cond.verify(tokenWithAlg("FOO256"), jwkSet(key.toPublicJWK())));
		assertTrue(e.getMessage().contains(NOT_A_JWS_ALG), e.getMessage());
	}

	@Test
	public void jweKeyManagementAlg_failsTheCondition() throws Exception {
		// These are JWE algorithms, not JWS ones, but KeyType.forAlgorithm() maps each onto a key type
		// ('dir' and 'A128KW' to oct, 'RSA-OAEP-256' to RSA, 'ECDH-ES' to EC), so a key-type test alone
		// would treat them as usable signing algorithms.
		ECKey key = new ECKeyGenerator(Curve.P_256).generate();
		for (String alg : new String[]{"dir", "A128KW", "RSA-OAEP-256", "ECDH-ES"}) {
			Throwable e = assertThrows(ConditionError.class,
				() -> cond.verify(tokenWithAlg(alg), jwkSet(key.toPublicJWK())), alg + " must be rejected");
			assertTrue(e.getMessage().contains(NOT_A_JWS_ALG), e.getMessage());
		}
	}

	@Test
	public void ecSignature_verifies() throws Exception {
		ECKey key = new ECKeyGenerator(Curve.P_256).generate();
		SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.ES256),
			new JWTClaimsSet.Builder().issuer("https://example.com").build());
		jwt.sign(new ECDSASigner(key));

		cond.verify(jwt.serialize(), jwkSet(key.toPublicJWK()));
	}

	@Test
	public void hmacSignature_verifies() throws Exception {
		// HS* algorithms are MAC, not signature, algorithms: JWSAlgorithm.Family.SIGNATURE does not
		// contain them, so the alg guard has to consult Family.HMAC_SHA as well or every MACed token in
		// the suite stops verifying.
		OctetSequenceKey key = new OctetSequenceKeyGenerator(256).generate();
		SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256),
			new JWTClaimsSet.Builder().issuer("https://example.com").build());
		jwt.sign(new MACSigner(key));

		cond.verify(jwt.serialize(), jwkSet(key));
	}

	@Test
	public void wrongKey_failsTheCondition() throws Exception {
		ECKey key = new ECKeyGenerator(Curve.P_256).generate();
		SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.ES256),
			new JWTClaimsSet.Builder().issuer("https://example.com").build());
		jwt.sign(new ECDSASigner(key));
		ECKey otherKey = new ECKeyGenerator(Curve.P_256).generate();

		assertThrows(ConditionError.class, () -> cond.verify(jwt.serialize(), jwkSet(otherKey.toPublicJWK())));
	}

	/**
	 * {@code verifyJwsSignature} is protected and the abstract class has no other entry point, so a
	 * concrete subclass is needed to reach it. {@code evaluate} is never called.
	 */
	private static class Harness extends AbstractVerifyJwsSignature {
		@Override
		public Environment evaluate(Environment env) {
			return env;
		}

		void verify(String token, JsonObject jwks) {
			verifyJwsSignature(token, jwks, "test token", false, "test");
		}
	}
}
