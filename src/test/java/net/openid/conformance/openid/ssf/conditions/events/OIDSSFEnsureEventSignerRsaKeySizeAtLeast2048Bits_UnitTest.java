package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class OIDSSFEnsureEventSignerRsaKeySizeAtLeast2048Bits_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private OIDSSFEnsureEventSignerRsaKeySizeAtLeast2048Bits createCondition() {
		OIDSSFEnsureEventSignerRsaKeySizeAtLeast2048Bits condition = new OIDSSFEnsureEventSignerRsaKeySizeAtLeast2048Bits();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.FAILURE);
		return condition;
	}

	private static RSAKey generateRsaKey(String kid, int bits) throws Exception {
		KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(bits);
		KeyPair keyPair = generator.generateKeyPair();
		return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
			.privateKey((RSAPrivateKey) keyPair.getPrivate())
			.keyUse(KeyUse.SIGNATURE)
			.keyID(kid)
			.build();
	}

	private void prepareEnv(RSAKey key, String setHeaderKid) {
		prepareEnv(List.of(key), setHeaderKid);
	}

	private void prepareEnv(List<? extends com.nimbusds.jose.jwk.JWK> keys, String setHeaderKid) {
		JsonObject jwks = JsonParser.parseString(new JWKSet(List.copyOf(keys)).toString(false)).getAsJsonObject();
		env.putObject("server_jwks", jwks);

		JsonObject header = new JsonObject();
		if (setHeaderKid != null) {
			header.addProperty("kid", setHeaderKid);
		}
		JsonObject setToken = new JsonObject();
		setToken.add("header", header);
		env.putObject("set_token", setToken);
	}

	@Test
	void passesFor2048BitKey() throws Exception {
		prepareEnv(generateRsaKey("k1", 2048), "k1");
		assertDoesNotThrow(() -> createCondition().execute(env));
	}

	@Test
	void failsForKeySmallerThan2048Bits() throws Exception {
		prepareEnv(generateRsaKey("k1", 1024), "k1");
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}

	@Test
	void failsWhenKidNotFoundInJwks() throws Exception {
		prepareEnv(generateRsaKey("k1", 2048), "unknown-kid");
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}

	@Test
	void passesWithoutKidWhenJwksContainsSingleRsaKey() throws Exception {
		prepareEnv(generateRsaKey("k1", 2048), null);
		assertDoesNotThrow(() -> createCondition().execute(env));
	}

	@Test
	void passesWithoutKidWhenAllCandidateRsaKeysAreLargeEnough() throws Exception {
		// kid-less SETs are verified against every candidate key (rotated JWKS)
		prepareEnv(List.of(generateRsaKey("k1", 2048), generateRsaKey("k2", 2048)), null);
		assertDoesNotThrow(() -> createCondition().execute(env));
	}

	@Test
	void failsWithoutKidWhenAnyCandidateRsaKeyIsTooSmall() throws Exception {
		prepareEnv(List.of(generateRsaKey("k1", 2048), generateRsaKey("k2", 1024)), null);
		assertThrows(ConditionError.class, () -> createCondition().execute(env));
	}

	@Test
	void kidResolutionSkipsNonRsaKeysSharingTheKid() throws Exception {
		// a JWKS may list an EC (e.g. encryption) key before the RSA signing key
		// under the same kid - the check must find the RSA one
		java.security.KeyPairGenerator ecGenerator = java.security.KeyPairGenerator.getInstance("EC");
		ecGenerator.initialize(new java.security.spec.ECGenParameterSpec("secp256r1"));
		java.security.KeyPair ecPair = ecGenerator.generateKeyPair();
		com.nimbusds.jose.jwk.ECKey ecKey = new com.nimbusds.jose.jwk.ECKey.Builder(
				com.nimbusds.jose.jwk.Curve.P_256, (java.security.interfaces.ECPublicKey) ecPair.getPublic())
			.keyID("k1")
			.keyUse(KeyUse.ENCRYPTION)
			.build();
		prepareEnv(List.of(ecKey, generateRsaKey("k1", 2048)), "k1");
		assertDoesNotThrow(() -> createCondition().execute(env));
	}
}
