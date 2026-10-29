package net.openid.conformance.condition.common;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

/**
 * Unlike {@link EnsureJwksHasNoPrivateOrSymmetricKeyMaterial}, this condition fails only on private
 * <em>asymmetric</em> key material; symmetric (oct) keys are tolerated (the PDP may MAC its
 * signed_metadata with a shared secret supplied as a JWK).
 */
@ExtendWith(MockitoExtension.class)
public class EnsureJwksHasNoPrivateAsymmetricKeyMaterial_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private EnsureJwksHasNoPrivateAsymmetricKeyMaterial cond;

	@BeforeEach
	public void setUp() {
		cond = new EnsureJwksHasNoPrivateAsymmetricKeyMaterial();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		env.putString("jwks_source_label", "PDP signing keys");
	}

	private void setJwks(String json) {
		env.putObject("jwks_to_validate", JsonParser.parseString(json).getAsJsonObject());
	}

	@Test
	public void publicKeySetPasses() {
		setJwks("""
			{ "keys": [ { "kty": "EC", "crv": "P-256",
			  "x": "hydCmuqjOVEFWuItZQuuZ74KGChEKb9qg_D0uvJCsEM",
			  "y": "g73kkv_85yvsOtTrkA5kg2as03d5HsxlqCkbPEpAYz4", "alg": "ES256" } ] }""");
		assertDoesNotThrow(() -> cond.evaluate(env));
	}

	@Test
	public void privateEcKeyFails() {
		setJwks("""
			{ "keys": [ { "kty": "EC", "crv": "P-256",
			  "x": "hydCmuqjOVEFWuItZQuuZ74KGChEKb9qg_D0uvJCsEM",
			  "y": "g73kkv_85yvsOtTrkA5kg2as03d5HsxlqCkbPEpAYz4",
			  "d": "_ypRKTu_hyy29nTdJb4xKX9bu7-ArYC191aeBuVNooA" } ] }""");
		ConditionError e = assertThrows(ConditionError.class, () -> cond.evaluate(env));
		assertTrue(e.getMessage().contains("PDP signing keys"));
		assertTrue(e.getMessage().contains("private asymmetric"));
	}

	@Test
	public void privateRsaKeyFails() {
		setJwks("""
			{ "keys": [ { "kty": "RSA", "n": "abc", "e": "AQAB",
			  "d": "private-exponent" } ] }""");
		assertThrows(ConditionError.class, () -> cond.evaluate(env));
	}

	// A private key on a curve the JOSE library cannot parse must still fail (raw JSON member scan).
	@Test
	public void privateKeyOnUnsupportedCurveFails() {
		setJwks("""
			{ "keys": [ { "kty": "EC", "crv": "BP-512",
			  "x": "qK-xJ65UZAZh6TCXhNJmVCqxqeB9FhQjraNj5c1I94_tITCucpOzhg5zj30j8AsclZNuogcpg-7VOXymsYTb6w",
			  "y": "KwUNahuNHNm6YQldhyhWoi3KAJC-PRzXmyh86mH7k6vAZn1Wz7XliRMOADXXnzx0cyjc06Ck2JItCUUVxDV__A",
			  "d": "anything" } ] }""");
		assertThrows(ConditionError.class, () -> cond.evaluate(env));
	}

	// Symmetric (oct) keys are allowed by this condition: no exception even though a secret 'k' is present.
	@Test
	public void symmetricKeyOnlyPasses() {
		setJwks("""
			{ "keys": [ { "kty": "oct", "k": "c2VjcmV0" } ] }""");
		assertDoesNotThrow(() -> cond.evaluate(env));
	}

	@Test
	public void publicAsymmetricPlusSymmetricPasses() {
		setJwks("""
			{ "keys": [
			  { "kty": "EC", "crv": "P-256",
			    "x": "hydCmuqjOVEFWuItZQuuZ74KGChEKb9qg_D0uvJCsEM",
			    "y": "g73kkv_85yvsOtTrkA5kg2as03d5HsxlqCkbPEpAYz4", "alg": "ES256" },
			  { "kty": "oct", "k": "c2VjcmV0" }
			] }""");
		assertDoesNotThrow(() -> cond.evaluate(env));
	}

	// A symmetric key does not mask a private asymmetric key also present in the set.
	@Test
	public void privateAsymmetricAlongsideSymmetricFails() {
		setJwks("""
			{ "keys": [
			  { "kty": "oct", "k": "c2VjcmV0" },
			  { "kty": "EC", "crv": "P-256",
			    "x": "hydCmuqjOVEFWuItZQuuZ74KGChEKb9qg_D0uvJCsEM",
			    "y": "g73kkv_85yvsOtTrkA5kg2as03d5HsxlqCkbPEpAYz4",
			    "d": "_ypRKTu_hyy29nTdJb4xKX9bu7-ArYC191aeBuVNooA" }
			] }""");
		assertThrows(ConditionError.class, () -> cond.evaluate(env));
	}

	// Only the private asymmetric key is reported; the symmetric key beside it is allowed.
	@Test
	public void privateAsymmetricAlongsideSymmetric_reportsOnlyTheAsymmetricKey() {
		setJwks("""
			{ "keys": [
			  { "kty": "oct", "k": "c2VjcmV0" },
			  { "kty": "EC", "crv": "P-256",
			    "x": "hydCmuqjOVEFWuItZQuuZ74KGChEKb9qg_D0uvJCsEM",
			    "y": "g73kkv_85yvsOtTrkA5kg2as03d5HsxlqCkbPEpAYz4",
			    "d": "_ypRKTu_hyy29nTdJb4xKX9bu7-ArYC191aeBuVNooA" }
			] }""");
		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("index 1"), e.getMessage());

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		verify(eventLog, atLeastOnce()).log(anyString(), captor.capture());
		JsonArray issues = captor.getAllValues().stream()
			.filter(m -> m.get("issues") instanceof JsonArray)
			.map(m -> (JsonArray) m.get("issues"))
			.findFirst()
			.orElseThrow();
		assertEquals(1, issues.size(), issues.toString());
		assertEquals(1, OIDFJSON.getInt(issues.get(0).getAsJsonObject().get("index")));
	}
}
