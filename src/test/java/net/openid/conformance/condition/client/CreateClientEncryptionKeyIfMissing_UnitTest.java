package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class CreateClientEncryptionKeyIfMissing_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CreateClientEncryptionKeyIfMissing cond;

	@BeforeEach
	public void setUp() throws Exception {

		cond = new CreateClientEncryptionKeyIfMissing();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);


	}

	@Test
	public void testExistingEncKey() {
		JsonObject jwks = JsonParser.parseString("""
				{
				  "keys": [
				    {
				      "kty": "RSA",
				      "d": "Go-fC48OvWNy-j-swFRAQOKMstXMycKgcg_DX04RvrY1KE76xcaU5VZx0pPOCPCobMH-XFOCDLa4dt67enUV1gSj7qmeG0r53NC3bj16D6YBAeP8cXrekRS4U-Z3OcfkMLlDatyXqt-eng17zEyoZ36mNTnuex_3VEJYxhWFM9hzyyppyXYk6NoEUu1atwFKMODZ588Dx10bmO2n50mztifiqOhwiTaspsnGsWc5kFu8SLWMKAJEHbmF88h6VOXFOVnE09-PnCSQD2Ux0MoG0pyX1M9OC7THU8xVxVnop3J3IzUIylo2seK2UPkIOVUlOk8CbKVIAc_auxZ9DiynwQ",
				      "e": "AQAB",
				      "use": "enc",
				      "kid": "TEST-KEY",
				      "alg": "RSA-OAEP",
				      "n": "o7m67H_EM0qOGkLRMSTOO8gUjEq61i-hueOfR3-8gN2c9Flz4fuiSRXwbAGORbL7geNfsda9KgLSSiRFLm-67AbJ85zplLu97Jtb24TrUd4dFYBxAq4ikz7Ue0BpkKQRizUSZhePr6hkHAeOlhEQeSOUm6Urdm14fjXdJSLAd2DHSIvutuRf4XwAjhmqIo8OCoO925JX7rLDW__cVDmv5Gm7nO-q4UuLxaQnZER3smWH5myQp_25b_0nmBh8ZPFf0eG8_MOQ9GTlffwMyI8U45xFQJMKqX6_bANtt7uaoV0yTB9AosooG8cEWN3e1YyghEMu9DDwpLZtL2heMGvpXw"
				    }
				  ]
				}""").getAsJsonObject();

		env.putObject("client", "jwks", jwks);

		cond.execute(env);

		// should still contain the key from the configuration
		JsonArray jwkArray = env.getElementFromObject("client", "jwks.keys").getAsJsonArray();
		assertThat(jwkArray.size()).isEqualTo(1);
		assertThat(OIDFJSON.getString(jwkArray.get(0).getAsJsonObject().get("kid"))).isEqualTo("TEST-KEY");
	}

	@Test
	public void testNoEncKey() {
		JsonObject jwks = JsonParser.parseString("""
			{
			  "keys": [
			    {
			      "kty": "RSA",
			      "d": "Go-fC48OvWNy-j-swFRAQOKMstXMycKgcg_DX04RvrY1KE76xcaU5VZx0pPOCPCobMH-XFOCDLa4dt67enUV1gSj7qmeG0r53NC3bj16D6YBAeP8cXrekRS4U-Z3OcfkMLlDatyXqt-eng17zEyoZ36mNTnuex_3VEJYxhWFM9hzyyppyXYk6NoEUu1atwFKMODZ588Dx10bmO2n50mztifiqOhwiTaspsnGsWc5kFu8SLWMKAJEHbmF88h6VOXFOVnE09-PnCSQD2Ux0MoG0pyX1M9OC7THU8xVxVnop3J3IzUIylo2seK2UPkIOVUlOk8CbKVIAc_auxZ9DiynwQ",
			      "e": "AQAB",
			      "use": "sig",
			      "kid": "TEST-KEY",
			      "alg": "RS256",
			      "n": "o7m67H_EM0qOGkLRMSTOO8gUjEq61i-hueOfR3-8gN2c9Flz4fuiSRXwbAGORbL7geNfsda9KgLSSiRFLm-67AbJ85zplLu97Jtb24TrUd4dFYBxAq4ikz7Ue0BpkKQRizUSZhePr6hkHAeOlhEQeSOUm6Urdm14fjXdJSLAd2DHSIvutuRf4XwAjhmqIo8OCoO925JX7rLDW__cVDmv5Gm7nO-q4UuLxaQnZER3smWH5myQp_25b_0nmBh8ZPFf0eG8_MOQ9GTlffwMyI8U45xFQJMKqX6_bANtt7uaoV0yTB9AosooG8cEWN3e1YyghEMu9DDwpLZtL2heMGvpXw"
			    }
			  ]
			}""").getAsJsonObject();

		env.putObject("client", "jwks", jwks);

		cond.execute(env);

		// should have two keys now
		JsonArray jwkArray = env.getElementFromObject("client", "jwks.keys").getAsJsonArray();
		assertThat(jwkArray.size()).isEqualTo(2);
		assertThat(OIDFJSON.getString(jwkArray.get(1).getAsJsonObject().get("kty"))).isEqualTo("EC");
	}

	@Test
	public void testClientJwks() {
		env.putObject("client", new JsonObject());

		cond.execute(env);

		// should have two keys now
		JsonArray jwkArray = env.getElementFromObject("client", "jwks.keys").getAsJsonArray();
		assertThat(jwkArray.size()).isEqualTo(1);
		assertThat(OIDFJSON.getString(jwkArray.get(0).getAsJsonObject().get("kty"))).isEqualTo("EC");
	}

	@Test
	public void testClientJwksRsa() {
		env.putObject("client", new JsonObject());
		env.putString("client", "authorization_encrypted_response_alg", "RSA-OAEP");

		cond.execute(env);

		// should have two keys now
		JsonArray jwkArray = env.getElementFromObject("client", "jwks.keys").getAsJsonArray();
		assertThat(jwkArray.size()).isEqualTo(1);
		assertThat(OIDFJSON.getString(jwkArray.get(0).getAsJsonObject().get("kty"))).isEqualTo("RSA");
	}
}
