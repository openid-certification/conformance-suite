package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jose.jwk.RSAKey;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWKUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class AugmentRealJwksWithDecoysTest {

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

	private void generateAndAddJwkToEnv(String alg) {
		JWK jwk = keyGenerator.createJwkForAlg(env, alg);
		JWKSet jwks = new JWKSet(jwk);
		env.putObject("server_jwks", JWKUtil.getPublicJwksAsJsonObject(jwks));
	}
}
