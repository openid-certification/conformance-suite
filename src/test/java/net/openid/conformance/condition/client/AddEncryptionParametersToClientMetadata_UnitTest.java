package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class AddEncryptionParametersToClientMetadata_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject client;

	private JsonObject jwks;

	private AddEncryptionParametersToClientMetadata cond;

	@BeforeEach
	public void setUp() throws Exception {

		cond = new AddEncryptionParametersToClientMetadata();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		jwks = JsonParser.parseString("""
				{
				  "keys": [
				    {
				      "kty": "RSA",
				      "d": "Go-fC48OvWNy-j-swFRAQOKMstXMycKgcg_DX04RvrY1KE76xcaU5VZx0pPOCPCobMH-XFOCDLa4dt67enUV1gSj7qmeG0r53NC3bj16D6YBAeP8cXrekRS4U-Z3OcfkMLlDatyXqt-eng17zEyoZ36mNTnuex_3VEJYxhWFM9hzyyppyXYk6NoEUu1atwFKMODZ588Dx10bmO2n50mztifiqOhwiTaspsnGsWc5kFu8SLWMKAJEHbmF88h6VOXFOVnE09-PnCSQD2Ux0MoG0pyX1M9OC7THU8xVxVnop3J3IzUIylo2seK2UPkIOVUlOk8CbKVIAc_auxZ9DiynwQ",
				      "e": "AQAB",
				      "use": "enc",
				      "kid": "TEST-KEY",
				      "n": "o7m67H_EM0qOGkLRMSTOO8gUjEq61i-hueOfR3-8gN2c9Flz4fuiSRXwbAGORbL7geNfsda9KgLSSiRFLm-67AbJ85zplLu97Jtb24TrUd4dFYBxAq4ikz7Ue0BpkKQRizUSZhePr6hkHAeOlhEQeSOUm6Urdm14fjXdJSLAd2DHSIvutuRf4XwAjhmqIo8OCoO925JX7rLDW__cVDmv5Gm7nO-q4UuLxaQnZER3smWH5myQp_25b_0nmBh8ZPFf0eG8_MOQ9GTlffwMyI8U45xFQJMKqX6_bANtt7uaoV0yTB9AosooG8cEWN3e1YyghEMu9DDwpLZtL2heMGvpXw"
				    }
				  ]
				}""").getAsJsonObject();

		client = JsonParser.parseString("""
				{
					"authorization_encrypted_response_alg": "RSA-OAEP-256",
					"authorization_encrypted_response_enc": "A128CBC-HS256"
				}
			""").getAsJsonObject();


		env.putObject("authorization_endpoint_request", "client_metadata", new JsonObject());
	}

	@Test
	public void testHappyPath() {

		env.putObject("client", client);
		env.putObject("client_public_jwks", jwks);

		cond.execute(env);
	}

	@Test
	public void testHappyPathNoAlgEncInConfig() {

		env.putObject("client_public_jwks", jwks);

		cond.execute(env);
	}

	@Test
	public void testNoEncKey() {
		assertThrows(ConditionError.class, () -> {

			JsonObject badjwks = JsonParser.parseString("""
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

			env.putObject("client", client);
			env.putObject("client_public_jwks", badjwks);

			cond.execute(env);
		});
	}
}
