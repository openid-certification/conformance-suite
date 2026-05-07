package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class VerifyKeyAttestationSignatureUsingConfigJwks_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private VerifyKeyAttestationSignatureUsingConfigJwks cond;

	@BeforeEach
	public void setUp() {
		cond = new VerifyKeyAttestationSignatureUsingConfigJwks();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void putKeyAttestationJwt(String rawJwt) {
		putKeyAttestationJwt(rawJwt, null);
	}

	private void putKeyAttestationJwt(String rawJwt, JsonArray x5c) {
		JsonObject header = new JsonObject();
		if (x5c != null) {
			header.add("x5c", x5c);
		}
		JsonObject keyAttestationJwt = new JsonObject();
		keyAttestationJwt.addProperty("value", rawJwt);
		keyAttestationJwt.add("header", header);
		env.putObject("vci", "key_attestation_jwt", keyAttestationJwt);
	}

	private void putConfiguredJwks(com.nimbusds.jose.jwk.JWK publicKey) {
		JsonObject jwks = new JsonObject();
		jwks.add("keys", JsonParser.parseString("[" + publicKey.toJSONString() + "]"));
		JsonObject config = new JsonObject();
		JsonObject vci = new JsonObject();
		vci.add("key_attestation_jwks", jwks);
		config.add("vci", vci);
		env.putObject("config", config);
	}

	@Test
	public void skipsWhenJwtHasX5cHeader() {
		JsonArray x5c = new JsonArray();
		x5c.add("ignored-cert");
		putKeyAttestationJwt("ignored", x5c);

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void verifiesSignatureUsingConfiguredJwks() throws Exception {
		ECKey signingKey = new ECKeyGenerator(Curve.P_256)
			.keyID("test-key")
			.generate();
		String jwt = signJwt(signingKey);

		putKeyAttestationJwt(jwt);
		putConfiguredJwks(signingKey.toPublicJWK());

		assertDoesNotThrow(() -> cond.execute(env));
	}

	@Test
	public void failsWhenJwksMissingFromConfig() throws Exception {
		ECKey signingKey = new ECKeyGenerator(Curve.P_256).generate();
		putKeyAttestationJwt(signJwt(signingKey));
		// No config.vci.key_attestation_jwks set.

		assertThrows(ConditionError.class, () -> cond.execute(env));
		assertEquals("invalid_proof", env.getString("vci", "credential_error_response.body.error"));
		assertTrue(env.getString("vci", "credential_error_response.body.error_description")
			.contains("'Key Attestation JWKS'"));
	}

	@Test
	public void failsWhenSignatureDoesNotMatch() throws Exception {
		ECKey signingKey = new ECKeyGenerator(Curve.P_256).generate();
		ECKey unrelatedKey = new ECKeyGenerator(Curve.P_256).generate();
		String jwt = signJwt(unrelatedKey);

		putKeyAttestationJwt(jwt);
		putConfiguredJwks(signingKey.toPublicJWK());

		assertThrows(ConditionError.class, () -> cond.execute(env));
		assertEquals("invalid_proof", env.getString("vci", "credential_error_response.body.error"));
	}

	@Test
	public void verifiesRsaSignedAttestationUsingConfiguredJwks() throws Exception {
		RSAKey signingKey = new RSAKeyGenerator(2048).keyID("test-rsa-key").generate();

		JWTClaimsSet claims = new JWTClaimsSet.Builder().issueTime(Date.from(Instant.now())).build();
		JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
			.type(new JOSEObjectType("key-attestation+jwt"))
			.build();
		SignedJWT signedJwt = new SignedJWT(header, claims);
		signedJwt.sign(new RSASSASigner(signingKey));

		putKeyAttestationJwt(signedJwt.serialize());
		putConfiguredJwks(signingKey.toPublicJWK());

		assertDoesNotThrow(() -> cond.execute(env));
	}

	private static String signJwt(ECKey signingKey) throws Exception {
		JWTClaimsSet claims = new JWTClaimsSet.Builder()
			.issueTime(Date.from(Instant.now()))
			.build();
		JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
			.type(new JOSEObjectType("key-attestation+jwt"))
			.build();
		SignedJWT jwt = new SignedJWT(header, claims);
		jwt.sign(new ECDSASigner(signingKey));
		return jwt.serialize();
	}
}
