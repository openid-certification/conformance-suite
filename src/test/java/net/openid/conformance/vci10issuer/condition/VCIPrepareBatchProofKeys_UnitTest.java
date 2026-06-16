package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWKUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.text.ParseException;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class VCIPrepareBatchProofKeys_UnitTest {

	private VCIPrepareBatchProofKeys cond;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private Environment env;

	@BeforeEach
	public void setUp() {
		cond = new VCIPrepareBatchProofKeys();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		env = new Environment();
		// PreGeneratedJwks requires owner_sub / owner_iss.
		env.putString("owner_sub", "unit-test-sub");
		env.putString("owner_iss", "unit-test-iss");
	}

	@Test
	public void testEvaluate_generatesOneKeyPerRequestedProof() throws ParseException {
		env.putInteger("vci_batch_size", 3);

		assertDoesNotThrow(() -> cond.execute(env));

		assertEquals(3, env.getInteger("vci_batch_requested_proof_count"));

		JsonObject jwksJson = env.getObject("vci_batch_proof_jwks");
		assertNotNull(jwksJson);
		JWKSet jwkSet = JWKUtil.parseJWKSet(jwksJson.toString());
		assertEquals(3, jwkSet.getKeys().size());

		Set<String> kids = new HashSet<>();
		for (JWK jwk : jwkSet.getKeys()) {
			assertEquals(KeyUse.SIGNATURE, jwk.getKeyUse());
			assertEquals(Curve.P_256, ((ECKey) jwk).getCurve());
			assertTrue(jwk.isPrivate(), "key must include the private part for proof signing");
			kids.add(jwk.getKeyID());
		}
		assertEquals(3, kids.size(), "keys must be distinct");
	}

	@Test
	public void testEvaluate_capsRequestedProofsAtTwenty() throws ParseException {
		env.putInteger("vci_batch_size", 50);

		assertDoesNotThrow(() -> cond.execute(env));

		assertEquals(20, env.getInteger("vci_batch_requested_proof_count"));
		JWKSet jwkSet = JWKUtil.parseJWKSet(env.getObject("vci_batch_proof_jwks").toString());
		assertEquals(20, jwkSet.getKeys().size());
	}

	@Test
	public void testEvaluate_publicJwksContainsNoPrivateParts() throws ParseException {
		env.putInteger("vci_batch_size", 2);

		assertDoesNotThrow(() -> cond.execute(env));

		JsonObject publicJwksJson = env.getObject("vci_batch_proof_public_jwks");
		assertNotNull(publicJwksJson);
		JWKSet publicJwkSet = JWKUtil.parseJWKSet(publicJwksJson.toString());
		assertEquals(2, publicJwkSet.getKeys().size());
		for (JWK jwk : publicJwkSet.getKeys()) {
			assertNull(((ECKey) jwk).getD(), "public JWKS must not contain private parts");
		}
	}
}
