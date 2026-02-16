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

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ValidateECJWKs_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ValidateECJWKs condition;

	@BeforeEach
	public void setUp() {
		condition = new ValidateECJWKs();
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
	public void acceptsPrivateJwksForStructuralValidation() throws Exception {
		var key = new ECKeyGenerator(Curve.P_256)
			.keyUse(KeyUse.SIGNATURE)
			.algorithm(JWSAlgorithm.ES256)
			.keyID("unit-test-key")
			.generate();

		JsonObject privateJwks = JWKUtil.getPrivateJwksAsJsonObject(new JWKSet(key));
		env.putObject("ec_jwks", privateJwks);

		condition.execute(env);
	}

	@Test
	public void rejectsMissingKeysArray() {
		JsonObject jwks = JsonParser.parseString("""
			{ "not_keys": [] }
			""").getAsJsonObject();
		env.putObject("ec_jwks", jwks);

		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	public void rejectsEcKeyMissingRequiredField() {
		JsonObject jwks = JsonParser.parseString("""
			{
			  "keys": [
			    {
			      "kty": "EC",
			      "crv": "P-256",
			      "x": "f83OJ3D2xF1Bg8vub9tLe1gHMzV76e8Tus9uPHvRVEU"
			    }
			  ]
			}
			""").getAsJsonObject();
		env.putObject("ec_jwks", jwks);

		assertThrows(ConditionError.class, () -> condition.execute(env));
	}

	@Test
	public void rejectsPaddedBase64UrlValues() {
		JsonObject jwks = JsonParser.parseString("""
			{
			  "keys": [
			    {
			      "kty": "EC",
			      "crv": "P-256",
			      "x": "f83OJ3D2xF1Bg8vub9tLe1gHMzV76e8Tus9uPHvRVEU=",
			      "y": "x_FEzRu9m36HLN_tue659LNpXW6pCyStikYjKIWI5a0="
			    }
			  ]
			}
			""").getAsJsonObject();
		env.putObject("ec_jwks", jwks);

		assertThrows(ConditionError.class, () -> condition.execute(env));
	}
}
