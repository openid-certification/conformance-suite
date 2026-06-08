package net.openid.conformance.condition.common;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class EnsureJwksHasNoPrivateOrSymmetricKeyMaterial_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsureJwksHasNoPrivateOrSymmetricKeyMaterial cond;

	@BeforeEach
	public void setUp() {
		cond = new EnsureJwksHasNoPrivateOrSymmetricKeyMaterial();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		env.putString("jwks_source_label", "client_metadata");
	}

	private void setJwks(String json) {
		env.putObject("jwks_to_validate", JsonParser.parseString(json).getAsJsonObject());
	}

	@Test
	public void publicKeySetPasses() {
		setJwks("""
			{ "keys": [ { "kty": "EC", "crv": "P-256",
			  "x": "hydCmuqjOVEFWuItZQuuZ74KGChEKb9qg_D0uvJCsEM",
			  "y": "g73kkv_85yvsOtTrkA5kg2as03d5HsxlqCkbPEpAYz4", "alg": "ECDH-ES" } ] }""");
		assertDoesNotThrow(() -> cond.evaluate(env));
	}

	@Test
	public void privateKeyFails() {
		setJwks("""
			{ "keys": [ { "kty": "EC", "crv": "P-256",
			  "x": "hydCmuqjOVEFWuItZQuuZ74KGChEKb9qg_D0uvJCsEM",
			  "y": "g73kkv_85yvsOtTrkA5kg2as03d5HsxlqCkbPEpAYz4",
			  "d": "_ypRKTu_hyy29nTdJb4xKX9bu7-ArYC191aeBuVNooA" } ] }""");
		ConditionError e = assertThrows(ConditionError.class, () -> cond.evaluate(env));
		assertTrue(e.getMessage().contains("client_metadata"));
	}

	// The key the original review finding was about: a private key on a curve the JOSE library cannot
	// parse must still fail (it would previously have been silently skipped).
	@Test
	public void privateKeyOnUnsupportedCurveFails() {
		setJwks("""
			{ "keys": [ { "kty": "EC", "crv": "BP-512",
			  "x": "qK-xJ65UZAZh6TCXhNJmVCqxqeB9FhQjraNj5c1I94_tITCucpOzhg5zj30j8AsclZNuogcpg-7VOXymsYTb6w",
			  "y": "KwUNahuNHNm6YQldhyhWoi3KAJC-PRzXmyh86mH7k6vAZn1Wz7XliRMOADXXnzx0cyjc06Ck2JItCUUVxDV__A",
			  "d": "anything" } ] }""");
		assertThrows(ConditionError.class, () -> cond.evaluate(env));
	}

	@Test
	public void symmetricKeyFails() {
		setJwks("""
			{ "keys": [ { "kty": "oct", "k": "c2VjcmV0" } ] }""");
		assertThrows(ConditionError.class, () -> cond.evaluate(env));
	}
}
