package net.openid.conformance.openid.federation;

import com.google.gson.JsonObject;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.openid.federation.client.GenerateEntityConfigurationForRPTest;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWKUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class GenerateEntityConfiguration_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private GenerateEntityConfigurationForOPTest generateForOpTest;
	private GenerateEntityConfigurationForRPTest generateForRpTest;

	@BeforeEach
	public void setUp() {
		generateForOpTest = new GenerateEntityConfigurationForOPTest();
		generateForOpTest.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		generateForRpTest = new GenerateEntityConfigurationForRPTest();
		generateForRpTest.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void generateEntityConfigurationForOpTestPublishesOnlyPublicJwks() throws Exception {
		JsonObject privateEcJwks = createPrivateEcJwks();
		env.putString("base_url", "https://demo.certification.example/op");
		env.putObject("rp_ec_jwks", privateEcJwks);

		generateForOpTest.execute(env);

		JsonObject publishedJwk = env.getObject("server")
			.getAsJsonObject("jwks")
			.getAsJsonArray("keys")
			.get(0)
			.getAsJsonObject();

		assertFalse(publishedJwk.has("d"));
		assertTrue(publishedJwk.has("x"));
		assertTrue(publishedJwk.has("y"));

		assertTrue(env.getObject("rp_ec_jwks").getAsJsonArray("keys")
			.get(0).getAsJsonObject().has("d"),
			"Original JWKS must retain private key");
	}

	@Test
	public void generateEntityConfigurationForRpTestPublishesOnlyPublicJwks() throws Exception {
		JsonObject privateEcJwks = createPrivateEcJwks();
		env.putString("base_url", "https://demo.certification.example/rp");
		env.putObject("op_ec_jwks", privateEcJwks);

		generateForRpTest.execute(env);

		JsonObject publishedJwk = env.getObject("server")
			.getAsJsonObject("jwks")
			.getAsJsonArray("keys")
			.get(0)
			.getAsJsonObject();

		assertFalse(publishedJwk.has("d"));
		assertTrue(publishedJwk.has("x"));
		assertTrue(publishedJwk.has("y"));

		assertTrue(env.getObject("op_ec_jwks").getAsJsonArray("keys")
			.get(0).getAsJsonObject().has("d"),
			"Original JWKS must retain private key");
	}

	private JsonObject createPrivateEcJwks() throws Exception {
		var key = new ECKeyGenerator(Curve.P_256)
			.keyUse(KeyUse.SIGNATURE)
			.algorithm(JWSAlgorithm.ES256)
			.keyID("unit-test-key")
			.generate();

		return JWKUtil.getPrivateJwksAsJsonObject(new JWKSet(key));
	}
}
