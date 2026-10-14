package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.AugmentRealJwksWithDecoys;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWKUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class SetRsaAltServerJwks_UnitTest {

	private static final String ALT_KID = "rsa-none-alt-arg";

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private SetRsaAltServerJwks cond;

	private JWKSet serverJwks;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new SetRsaAltServerJwks();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		JWK signingKey = new RSAKeyGenerator(2048).keyUse(KeyUse.SIGNATURE).algorithm(JWSAlgorithm.PS256).keyID("real-signing-key").generate();
		serverJwks = new JWKSet(signingKey);
		env.putObject("server_jwks", JWKUtil.getPrivateJwksAsJsonObject(serverJwks));
	}

	@Test
	public void appendsAltKeyToExistingPublishedKeys() {
		JsonObject publishedJwks = JWKUtil.getPublicJwksAsJsonObject(serverJwks);
		publishedJwks.getAsJsonArray("keys").add(JsonParser.parseString("""
			{"kty": "OIDF-CONFORMANCE-UNSUPPORTED", "kid": "published-only-key", "use": "sig"}""").getAsJsonObject());
		env.putObject("server_public_jwks", publishedJwks);

		cond.execute(env);

		assertEquals(List.of("real-signing-key", "published-only-key", ALT_KID), kids(env.getObject("server_public_jwks")));
		assertNoPrivateKeyMembers(env.getObject("server_public_jwks"));
		assertAltPrivateKeyStored();
	}

	@Test
	public void derivesPublishedKeysFromServerJwksWhenServerPublicJwksAbsent() {
		cond.execute(env);

		assertEquals(List.of("real-signing-key", ALT_KID), kids(env.getObject("server_public_jwks")));
		assertNoPrivateKeyMembers(env.getObject("server_public_jwks"));
		assertAltPrivateKeyStored();
	}

	@Test
	public void failsWhenServerPublicJwksHasNoKeysArray() {
		env.putObject("server_public_jwks", new JsonObject());

		ConditionError e = assertThrows(ConditionError.class, () -> cond.execute(env));
		assertTrue(e.getMessage().contains("does not contain a 'keys' array"), e.getMessage());
	}

	@Test
	public void decoyAltAndUnusableKeysAreAllPublished() {
		env.putObject("server_public_jwks", JWKUtil.getPublicJwksAsJsonObject(serverJwks));
		// PreGeneratedJwks, used to create the decoys, requires owner_sub / owner_iss in env.
		env.putString("owner_sub", "test-sub");
		env.putString("owner_iss", "test-iss");

		AugmentRealJwksWithDecoys decoys = new AugmentRealJwksWithDecoys();
		decoys.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		AddUnusableKeysToServerPublicJwks unusable = new AddUnusableKeysToServerPublicJwks();
		unusable.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		decoys.execute(env);
		cond.execute(env);
		unusable.execute(env);

		JsonArray keys = env.getObject("server_public_jwks").getAsJsonArray("keys");
		assertEquals(List.of(
				"real-signing-key:EdDSA", "real-signing-key:PS256", "real-signing-key:ES256", ALT_KID + ":",
				"unusable-pq-sig-key:ML-DSA-9999", "unusable-unknown-kty-key:OIDF-CONFORMANCE-UNSUPPORTED",
				"unusable-rsa-unknown-alg-key:RS9999", "unusable-ec-unknown-alg-key:ES9999", "unusable-ec-unknown-crv-key:"),
			kidsAndAlgs(keys));
	}

	private void assertAltPrivateKeyStored() {
		JsonObject altKey = env.getObject("server_alt_jwks").getAsJsonArray("keys").get(0).getAsJsonObject();
		assertEquals(ALT_KID, OIDFJSON.getString(altKey.get("kid")));
		assertTrue(altKey.has("d"));
	}

	private static List<String> kids(JsonObject jwks) {
		List<String> kids = new ArrayList<>();
		for (JsonElement key : jwks.getAsJsonArray("keys")) {
			kids.add(OIDFJSON.getString(key.getAsJsonObject().get("kid")));
		}
		return kids;
	}

	private static List<String> kidsAndAlgs(JsonArray keys) {
		List<String> result = new ArrayList<>();
		for (JsonElement key : keys) {
			JsonObject o = key.getAsJsonObject();
			String alg = o.has("alg") ? OIDFJSON.getString(o.get("alg")) : "";
			result.add(OIDFJSON.getString(o.get("kid")) + ":" + alg);
		}
		return result;
	}

	private static void assertNoPrivateKeyMembers(JsonObject jwks) {
		for (JsonElement key : jwks.getAsJsonArray("keys")) {
			for (String member : List.of("d", "p", "q", "dp", "dq", "qi")) {
				assertFalse(key.getAsJsonObject().has(member), "published key must not contain private member " + member);
			}
		}
	}
}
