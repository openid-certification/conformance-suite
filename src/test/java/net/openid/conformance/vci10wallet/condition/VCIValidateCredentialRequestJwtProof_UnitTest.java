package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWTUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class VCIValidateCredentialRequestJwtProof_UnitTest {

	private static final String ISSUER = "https://issuer.example.com/test/a/abc/";
	private static final String NONCE = "test-nonce-value";

	private VCIValidateCredentialRequestJwtProof cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new VCIValidateCredentialRequestJwtProof();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		env = new Environment();

		env.putString("proof_type", "jwt");
		env.putString("credential_configuration_id", "test-config");
		JsonObject proofTypesSupported = new JsonObject();
		proofTypesSupported.add("jwt", new JsonObject());
		JsonObject credentialConfiguration = new JsonObject();
		credentialConfiguration.add("proof_types_supported", proofTypesSupported);
		env.putObject("credential_configuration", credentialConfiguration);

		JsonObject issuerMetadata = new JsonObject();
		issuerMetadata.addProperty("credential_issuer", ISSUER);
		env.putObject("credential_issuer_metadata", issuerMetadata);

		env.putString("credential_issuer_nonce", NONCE);
	}

	private String createProofJwt(ECKey signingKey, ECKey headerKey, String nonce) throws Exception {
		SignedJWT jwt = new SignedJWT(
			new JWSHeader.Builder(JWSAlgorithm.ES256)
				.type(new JOSEObjectType("openid4vci-proof+jwt"))
				.jwk(headerKey.toPublicJWK())
				.build(),
			new JWTClaimsSet.Builder()
				.audience(ISSUER)
				.issueTime(new Date())
				.claim("nonce", nonce)
				.build());
		jwt.sign(new ECDSASigner(signingKey));
		return jwt.serialize();
	}

	private void putProofJwts(String... jwts) throws Exception {
		JsonArray items = new JsonArray();
		for (String jwt : jwts) {
			items.add(JWTUtil.jwtStringToJsonObjectForEnvironment(jwt));
		}
		JsonObject wrapper = new JsonObject();
		wrapper.add("items", items);
		env.putObject("proof_jwts", wrapper);
		env.putObject("proof_jwt", items.get(0).getAsJsonObject().deepCopy());
	}

	@Test
	public void testEvaluate_validBatchOfProofsPasses() throws Exception {
		ECKey key1 = new ECKeyGenerator(Curve.P_256).generate();
		ECKey key2 = new ECKeyGenerator(Curve.P_256).generate();
		putProofJwts(
			createProofJwt(key1, key1, NONCE),
			createProofJwt(key2, key2, NONCE));

		assertDoesNotThrow(() -> cond.execute(env));

		assertNull(env.getString("credential_issuer_nonce"), "nonce must be invalidated after validation");

		// first proof key kept at proof_jwt.jwk for backward compatibility
		assertNotNull(env.getElementFromObject("proof_jwt", "jwk"));
		assertEquals(key1.toPublicJWK().computeThumbprint().toString(),
			com.nimbusds.jose.jwk.JWK.parse(
				env.getElementFromObject("proof_jwt", "jwk").getAsJsonObject().toString())
				.computeThumbprint().toString());

		// each item carries its resolved key
		JsonArray items = env.getObject("proof_jwts").getAsJsonArray("items");
		assertEquals(2, items.size());
		assertEquals(key2.toPublicJWK().computeThumbprint().toString(),
			com.nimbusds.jose.jwk.JWK.parse(
				items.get(1).getAsJsonObject().getAsJsonObject("jwk").toString())
				.computeThumbprint().toString());
	}

	@Test
	public void testEvaluate_singleProofWithoutProofJwtsWrapperPasses() throws Exception {
		ECKey key1 = new ECKeyGenerator(Curve.P_256).generate();
		String jwt = createProofJwt(key1, key1, NONCE);
		env.putObject("proof_jwt", JWTUtil.jwtStringToJsonObjectForEnvironment(jwt));

		assertDoesNotThrow(() -> cond.execute(env));
		assertNull(env.getString("credential_issuer_nonce"));
	}

	@Test
	public void testEvaluate_invalidSignatureOnSecondProofFailsNamingIndex() throws Exception {
		ECKey key1 = new ECKeyGenerator(Curve.P_256).generate();
		ECKey key2 = new ECKeyGenerator(Curve.P_256).generate();
		ECKey otherKey = new ECKeyGenerator(Curve.P_256).generate();
		putProofJwts(
			createProofJwt(key1, key1, NONCE),
			// signed with a key that doesn't match the embedded jwk -> signature check fails
			createProofJwt(otherKey, key2, NONCE));

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("proof 2 of 2"),
			"error should name the failing proof: " + e.getMessage());
	}

	@Test
	public void testEvaluate_wrongNonceOnSecondProofFailsAndKeepsNonce() throws Exception {
		ECKey key1 = new ECKeyGenerator(Curve.P_256).generate();
		ECKey key2 = new ECKeyGenerator(Curve.P_256).generate();
		putProofJwts(
			createProofJwt(key1, key1, NONCE),
			createProofJwt(key2, key2, "some-other-nonce"));

		assertThrows(ConditionError.class, () -> cond.execute(env));
		assertEquals(NONCE, env.getString("credential_issuer_nonce"),
			"nonce must not be consumed when validation fails");
	}
}
