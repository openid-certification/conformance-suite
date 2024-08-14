package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.gen.JWKGenerator;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWKUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class AugmentRealJwksWithDecoysTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private AugmentRealJwksWithDecoys augmentRealJwksWithDecoys;

	@BeforeEach
	public void setUp() throws Exception {
		JsonObject server = new JsonObject();
		server.addProperty("jwks_uri", "https://as/jwks");
		env.putObject("server", server);
		env.putString("base_url", "https://as");

		var keyGenerator = new AbstractGenerateKey() {
			@Override
			public Environment evaluate(Environment env) {
				return null;
			}

			@Override
			protected JWKGenerator<? extends JWK> onConfigure(JWKGenerator<? extends JWK> generator) {
				// use same kid for all keys
				generator.keyID("testKey");
				return generator;
			}
		};

		JWK ps256 = keyGenerator.createJwkForAlg("PS256");
		JWKSet jwks = new JWKSet(ps256);

		env.putObject("server_jwks", JWKUtil.getPublicJwksAsJsonObject(jwks));

		augmentRealJwksWithDecoys = new AugmentRealJwksWithDecoys();
		augmentRealJwksWithDecoys.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO, "server", "base_url", "server_public_jwks");
	}

	@Test
	public void shouldGenerateDecoyKeysAndUpdateJwksUri() {

		augmentRealJwksWithDecoys.evaluate(env);

		JsonObject serverPublicJwksDecoy = env.getObject("server_public_jwks_decoy");
		assertThat(serverPublicJwksDecoy).isNotNull();
		String jwksUri = env.getString("server", "jwks_uri");
		assertThat(jwksUri).isEqualTo("https://as/jwks_decoy");
	}
}
