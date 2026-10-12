package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWKUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
public class AugmentRealJwksWithDecoys_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	AbstractGenerateKey keyGenerator;

	private AugmentRealJwksWithDecoys augmentRealJwksWithDecoys;

	@BeforeEach
	public void setUp() throws Exception {
		JsonObject server = new JsonObject();
		env.putObject("server", server);
		// PreGeneratedJwks requires owner_sub / owner_iss in env.
		env.putString("owner_sub", "test-sub");
		env.putString("owner_iss", "test-iss");

		keyGenerator = new AbstractGenerateKey() {
			@Override
			public Environment evaluate(Environment env) {
				return null;
			}

			// use same kid for all keys

			@Override
			protected ECKey.Builder onConfigureEc(ECKey.Builder b) throws JOSEException {
				return b.keyID("testKey");
			}

			@Override
			protected OctetKeyPair.Builder onConfigureOkp(OctetKeyPair.Builder b) throws JOSEException {
				return b.keyID("testKey");
			}

			@Override
			protected RSAKey.Builder onConfigureRsa(RSAKey.Builder b) throws JOSEException {
				return b.keyID("testKey");
			}
		};

		augmentRealJwksWithDecoys = new AugmentRealJwksWithDecoys();
		augmentRealJwksWithDecoys.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO, "server", "base_url", "server_public_jwks");
	}

	@ParameterizedTest
	@CsvSource(value = {"PS256:EdDSA,PS256,ES256", "EdDSA:ES256,EdDSA,PS256", "ES256:EdDSA,ES256,PS256"}, delimiter = ':')
	public void shouldGenerateDecoyKeys(String algInput, String expectedAlgWithDecoys) {

		generateAndAddJwkToEnv(algInput);

		augmentRealJwksWithDecoys.evaluate(env);

		JsonObject serverPublicJwksDecoy = env.getObject("server_public_jwks");
		assertThat(serverPublicJwksDecoy).isNotNull();

		JsonArray keys = serverPublicJwksDecoy.getAsJsonArray("keys");
		String actualAlgs = Stream.of(keys.get(0), keys.get(1), keys.get(2))
			.map(entry -> OIDFJSON.getString(entry.getAsJsonObject().get("alg")))
			.collect(Collectors.joining(","));
		assertThat(actualAlgs).isEqualTo(expectedAlgWithDecoys);
	}

	@Test
	public void shouldInsertDecoysAroundRealKeyAndKeepOtherPublishedKeys() throws Exception {
		JWK signingKey = keyGenerator.createJwkForAlg(env, "PS256");
		JWK encryptionKey = new RSAKeyGenerator(AbstractGenerateKey.RSA_KEY_SIZE).keyUse(KeyUse.ENCRYPTION).keyID("enc-key").generate();
		JWKSet serverJwks = new JWKSet(List.of(signingKey, encryptionKey));
		env.putObject("server_jwks", JWKUtil.getPrivateJwksAsJsonObject(serverJwks));

		JsonObject publishedJwks = JWKUtil.getPublicJwksAsJsonObject(serverJwks);
		// a key Nimbus cannot parse, as published by AddUnusableKeysToServerPublicJwks
		publishedJwks.getAsJsonArray("keys").add(JsonParser.parseString("""
			{"kty": "OIDF-CONFORMANCE-UNSUPPORTED", "kid": "unusable-unknown-kty-key", "use": "sig"}""").getAsJsonObject());
		env.putObject("server_public_jwks", publishedJwks);
		JsonObject realPublicKey = publishedJwks.getAsJsonArray("keys").get(0).getAsJsonObject().deepCopy();

		augmentRealJwksWithDecoys.execute(env);

		JsonArray keys = env.getObject("server_public_jwks").getAsJsonArray("keys");
		assertThat(keys.size()).isEqualTo(5);
		assertThat(kidAndAlg(keys.get(0))).isEqualTo("testKey:EdDSA");
		assertThat(keys.get(1)).isEqualTo(realPublicKey);
		assertThat(kidAndAlg(keys.get(2))).isEqualTo("testKey:ES256");
		assertThat(OIDFJSON.getString(keys.get(3).getAsJsonObject().get("kid"))).isEqualTo("enc-key");
		assertThat(OIDFJSON.getString(keys.get(4).getAsJsonObject().get("kid"))).isEqualTo("unusable-unknown-kty-key");
		assertNoPrivateKeyMembers(keys);
	}

	@Test
	public void shouldDerivePublishedKeysFromServerJwksWhenServerPublicJwksAbsent() throws Exception {
		JWK signingKey = keyGenerator.createJwkForAlg(env, "ES256");
		JWK encryptionKey = new RSAKeyGenerator(AbstractGenerateKey.RSA_KEY_SIZE).keyUse(KeyUse.ENCRYPTION).keyID("enc-key").generate();
		env.putObject("server_jwks", JWKUtil.getPrivateJwksAsJsonObject(new JWKSet(List.of(signingKey, encryptionKey))));

		augmentRealJwksWithDecoys.execute(env);

		JsonArray keys = env.getObject("server_public_jwks").getAsJsonArray("keys");
		assertThat(keys.size()).isEqualTo(4);
		assertThat(kidAndAlg(keys.get(0))).isEqualTo("testKey:EdDSA");
		assertThat(kidAndAlg(keys.get(1))).isEqualTo("testKey:ES256");
		assertThat(kidAndAlg(keys.get(2))).isEqualTo("testKey:PS256");
		assertThat(OIDFJSON.getString(keys.get(3).getAsJsonObject().get("kid"))).isEqualTo("enc-key");
		assertNoPrivateKeyMembers(keys);
	}

	@Test
	public void shouldSkipDecoysForSigningKeyWithoutKid() throws Exception {
		JWK signingKey = new ECKeyGenerator(Curve.P_256).keyUse(KeyUse.SIGNATURE).algorithm(JWSAlgorithm.ES256).generate();
		JWKSet serverJwks = new JWKSet(signingKey);
		env.putObject("server_jwks", JWKUtil.getPrivateJwksAsJsonObject(serverJwks));
		JsonObject publishedJwks = JWKUtil.getPublicJwksAsJsonObject(serverJwks);
		env.putObject("server_public_jwks", publishedJwks.deepCopy());

		augmentRealJwksWithDecoys.execute(env);

		assertThat(env.getObject("server_public_jwks")).isEqualTo(publishedJwks);
	}

	@Test
	public void shouldFailWhenServerPublicJwksHasNoKeysArray() {
		generateAndAddJwkToEnv("PS256");
		env.putObject("server_public_jwks", new JsonObject());

		assertThatThrownBy(() -> augmentRealJwksWithDecoys.execute(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("does not contain a 'keys' array");
	}

	@Test
	public void shouldFailWhenServerPublicJwksDoesNotContainSigningKey() throws Exception {
		generateAndAddJwkToEnv("PS256");
		JWK otherKey = new RSAKeyGenerator(AbstractGenerateKey.RSA_KEY_SIZE).keyUse(KeyUse.ENCRYPTION).keyID("enc-key").generate();
		env.putObject("server_public_jwks", JWKUtil.getPublicJwksAsJsonObject(new JWKSet(otherKey)));

		assertThatThrownBy(() -> augmentRealJwksWithDecoys.execute(env))
			.isInstanceOf(ConditionError.class)
			.hasMessageContaining("does not contain the server signing key");
	}

	private static String kidAndAlg(JsonElement key) {
		JsonObject o = key.getAsJsonObject();
		return OIDFJSON.getString(o.get("kid")) + ":" + OIDFJSON.getString(o.get("alg"));
	}

	private static void assertNoPrivateKeyMembers(JsonArray keys) {
		for (JsonElement key : keys) {
			assertThat(key.getAsJsonObject().keySet()).doesNotContain("d", "p", "q", "dp", "dq", "qi");
		}
	}

	private void generateAndAddJwkToEnv(String alg) {
		JWK jwk = keyGenerator.createJwkForAlg(env, alg);
		JWKSet jwks = new JWKSet(jwk);
		env.putObject("server_jwks", JWKUtil.getPublicJwksAsJsonObject(jwks));
	}
}
