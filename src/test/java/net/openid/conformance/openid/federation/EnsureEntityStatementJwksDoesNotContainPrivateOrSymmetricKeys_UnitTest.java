package net.openid.conformance.openid.federation;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWKUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class EnsureEntityStatementJwksDoesNotContainPrivateOrSymmetricKeys_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private EnsureEntityStatementJwksDoesNotContainPrivateOrSymmetricKeys condition;

	@BeforeEach
	public void setUp() {
		condition = new EnsureEntityStatementJwksDoesNotContainPrivateOrSymmetricKeys();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void acceptsPublicJwks() throws Exception {
		var key = new ECKeyGenerator(Curve.P_256)
			.keyUse(KeyUse.SIGNATURE)
			.algorithm(JWSAlgorithm.ES256)
			.keyID("unit-test-key")
			.generate();

		JsonObject publicJwks = JWKUtil.getPublicJwksAsJsonObject(new JWKSet(key));
		env.putObject("ec_jwks", publicJwks);

		condition.execute(env);
	}

	@Test
	public void rejectsPrivateJwks() throws Exception {
		var key = new ECKeyGenerator(Curve.P_256)
			.keyUse(KeyUse.SIGNATURE)
			.algorithm(JWSAlgorithm.ES256)
			.keyID("unit-test-key")
			.generate();

		JsonObject privateJwks = JWKUtil.getPrivateJwksAsJsonObject(new JWKSet(key));
		env.putObject("ec_jwks", privateJwks);

		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	public void rejectsSymmetricJwks() {
		JsonObject symmetricJwks = JsonParser.parseString("""
			{
			  "keys": [
			    {
			      "kty": "oct",
			      "alg": "A128KW",
			      "k": "GawgguFyGrWKav7AX4VKUg"
			    }
			  ]
			}
			""").getAsJsonObject();
		env.putObject("ec_jwks", symmetricJwks);

		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	public void rejectsMixedPublicAndPrivateKeys() throws Exception {
		var privateKey = new ECKeyGenerator(Curve.P_256)
			.keyUse(KeyUse.SIGNATURE)
			.algorithm(JWSAlgorithm.ES256)
			.keyID("private-key")
			.generate();

		var publicKey = new ECKeyGenerator(Curve.P_256)
			.keyUse(KeyUse.SIGNATURE)
			.algorithm(JWSAlgorithm.ES256)
			.keyID("public-key")
			.generate()
			.toPublicJWK();

		JWKSet mixedSet = new JWKSet(List.of(publicKey, privateKey));
		JsonObject mixedJwks = JWKUtil.getPrivateJwksAsJsonObject(mixedSet);
		env.putObject("ec_jwks", mixedJwks);

		assertThrows(ConditionError.class, () -> condition.execute(env));
	}
}
