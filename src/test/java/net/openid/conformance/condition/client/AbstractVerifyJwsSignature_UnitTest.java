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
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jose.util.X509CertUtils;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

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

	private static SignedJWT es256WithThumbprint(ECKey key, String kid, Base64URL x5tS256) throws Exception {
		SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(kid).x509CertSHA256Thumbprint(x5tS256).build(),
			new JWTClaimsSet.Builder().issuer("https://example.com").build());
		jwt.sign(new ECDSASigner(key));
		return jwt;
	}

	@Test
	public void headerThumbprintWithKeyWithoutThumbprint_verifies() throws Exception {
		// A key without x5t#S256 cannot be ruled out by the header's thumbprint; this used to
		// dereference the key's missing thumbprint and fail with an internal NullPointerException.
		ECKey key = new ECKeyGenerator(Curve.P_256).generate();
		SignedJWT jwt = es256WithThumbprint(key, null, Base64URL.encode("thumbprint"));

		cond.verify(jwt.serialize(), jwkSet(key.toPublicJWK()));
	}

	@Test
	public void headerThumbprintWithKidAndKeyWithoutThumbprint_verifies() throws Exception {
		ECKey key = new ECKeyGenerator(Curve.P_256).keyID("k1").generate();
		SignedJWT jwt = es256WithThumbprint(key, "k1", Base64URL.encode("thumbprint"));

		cond.verify(jwt.serialize(), jwkSet(key.toPublicJWK()));
	}

	@Test
	public void headerThumbprintMatchingKeyThumbprint_verifies() throws Exception {
		Base64URL thumbprint = Base64URL.encode("thumbprint");
		ECKey key = new ECKeyGenerator(Curve.P_256).generate();
		SignedJWT jwt = es256WithThumbprint(key, null, thumbprint);
		ECKey publicKey = new ECKey.Builder(key.toPublicJWK()).x509CertSHA256Thumbprint(thumbprint).build();

		cond.verify(jwt.serialize(), jwkSet(publicKey));
	}

	@Test
	public void headerThumbprintDifferentFromKeyThumbprint_failsTheCondition() throws Exception {
		ECKey key = new ECKeyGenerator(Curve.P_256).generate();
		SignedJWT jwt = es256WithThumbprint(key, null, Base64URL.encode("thumbprint"));
		ECKey publicKey = new ECKey.Builder(key.toPublicJWK()).x509CertSHA256Thumbprint(Base64URL.encode("other")).build();

		assertThrows(ConditionError.class, () -> cond.verify(jwt.serialize(), jwkSet(publicKey)));
	}

	/** The public key with a self-signed certificate for it as its only 'x5c' entry, and no 'x5t#S256'. */
	private static ECKey publicKeyWithCertificate(ECKey key) throws Exception {
		X500Name name = new X500Name("CN=test");
		Instant now = Instant.now();
		X509CertificateHolder holder = new JcaX509v3CertificateBuilder(name, BigInteger.ONE,
			Date.from(now.minus(1, ChronoUnit.DAYS)), Date.from(now.plus(1, ChronoUnit.DAYS)), name, key.toECPublicKey())
			.build(new JcaContentSignerBuilder("SHA256withECDSA").build(key.toECPrivateKey()));
		return new ECKey.Builder(key.toPublicJWK()).x509CertChain(List.of(Base64.encode(holder.getEncoded()))).build();
	}

	private static Base64URL certificateThumbprint(ECKey keyWithCertificate) {
		return X509CertUtils.computeSHA256Thumbprint(keyWithCertificate.getParsedX509CertChain().get(0));
	}

	@Test
	public void headerThumbprintMatchingKeyCertificate_verifies() throws Exception {
		ECKey key = new ECKeyGenerator(Curve.P_256).generate();
		ECKey publicKey = publicKeyWithCertificate(key);
		SignedJWT jwt = es256WithThumbprint(key, null, certificateThumbprint(publicKey));

		cond.verify(jwt.serialize(), jwkSet(publicKey));
	}

	@Test
	public void headerThumbprintWithKidMatchingKeyCertificate_verifies() throws Exception {
		ECKey key = new ECKeyGenerator(Curve.P_256).keyID("k1").generate();
		ECKey publicKey = publicKeyWithCertificate(key);
		SignedJWT jwt = es256WithThumbprint(key, "k1", certificateThumbprint(publicKey));

		cond.verify(jwt.serialize(), jwkSet(publicKey));
	}

	@Test
	public void headerThumbprintDifferentFromKeyCertificate_failsTheCondition() throws Exception {
		// The key has no x5t#S256 of its own, but its certificate does not have the header's thumbprint.
		ECKey key = new ECKeyGenerator(Curve.P_256).generate();
		SignedJWT jwt = es256WithThumbprint(key, null, Base64URL.encode(new byte[32]));

		assertThrows(ConditionError.class, () -> cond.verify(jwt.serialize(), jwkSet(publicKeyWithCertificate(key))));
	}

	@Test
	public void headerThumbprintWithKidDifferentFromKeyCertificate_failsTheCondition() throws Exception {
		ECKey key = new ECKeyGenerator(Curve.P_256).keyID("k1").generate();
		SignedJWT jwt = es256WithThumbprint(key, "k1", Base64URL.encode(new byte[32]));

		assertThrows(ConditionError.class, () -> cond.verify(jwt.serialize(), jwkSet(publicKeyWithCertificate(key))));
	}

	@Test
	public void headerThumbprintMatchingKeyCertificateButNotKeyThumbprint_failsTheCondition() throws Exception {
		ECKey key = new ECKeyGenerator(Curve.P_256).generate();
		ECKey withCertificate = publicKeyWithCertificate(key);
		SignedJWT jwt = es256WithThumbprint(key, null, certificateThumbprint(withCertificate));
		ECKey publicKey = new ECKey.Builder(withCertificate).x509CertSHA256Thumbprint(Base64URL.encode("other")).build();

		assertThrows(ConditionError.class, () -> cond.verify(jwt.serialize(), jwkSet(publicKey)));
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
