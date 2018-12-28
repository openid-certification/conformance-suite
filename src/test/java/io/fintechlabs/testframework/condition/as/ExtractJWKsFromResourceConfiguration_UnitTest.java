package io.fintechlabs.testframework.condition.as;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import static org.junit.Assert.assertEquals;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class ExtractJWKsFromResourceConfiguration_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject resource;

	private JsonObject jwks;

	private JsonObject publicJwks;

	private ExtractJWKsFromResourceConfiguration cond;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new ExtractJWKsFromResourceConfiguration("UNIT-TEST", eventLog, ConditionResult.INFO);

		jwks = new JsonParser().parse("{\n" +
			"  \"keys\": [\n" +
			"    {\n" +
			"      \"kty\": \"RSA\",\n" +
			"      \"d\": \"Go-fC48OvWNy-j-swFRAQOKMstXMycKgcg_DX04RvrY1KE76xcaU5VZx0pPOCPCobMH-XFOCDLa4dt67enUV1gSj7qmeG0r53NC3bj16D6YBAeP8cXrekRS4U-Z3OcfkMLlDatyXqt-eng17zEyoZ36mNTnuex_3VEJYxhWFM9hzyyppyXYk6NoEUu1atwFKMODZ588Dx10bmO2n50mztifiqOhwiTaspsnGsWc5kFu8SLWMKAJEHbmF88h6VOXFOVnE09-PnCSQD2Ux0MoG0pyX1M9OC7THU8xVxVnop3J3IzUIylo2seK2UPkIOVUlOk8CbKVIAc_auxZ9DiynwQ\",\n" +
			"      \"e\": \"AQAB\",\n" +
			"      \"use\": \"sig\",\n" +
			"      \"kid\": \"TEST-KEY\",\n" +
			"      \"alg\": \"RS256\",\n" +
			"      \"n\": \"o7m67H_EM0qOGkLRMSTOO8gUjEq61i-hueOfR3-8gN2c9Flz4fuiSRXwbAGORbL7geNfsda9KgLSSiRFLm-67AbJ85zplLu97Jtb24TrUd4dFYBxAq4ikz7Ue0BpkKQRizUSZhePr6hkHAeOlhEQeSOUm6Urdm14fjXdJSLAd2DHSIvutuRf4XwAjhmqIo8OCoO925JX7rLDW__cVDmv5Gm7nO-q4UuLxaQnZER3smWH5myQp_25b_0nmBh8ZPFf0eG8_MOQ9GTlffwMyI8U45xFQJMKqX6_bANtt7uaoV0yTB9AosooG8cEWN3e1YyghEMu9DDwpLZtL2heMGvpXw\"\n" +
			"    }\n" +
			"  ]\n" +
			"}").getAsJsonObject();

		jwks = new JsonParser().parse("{\n" +
			"  \"keys\": [\n" +
			"    {\n" +
			"      \"kty\": \"RSA\",\n" +
			"      \"e\": \"AQAB\",\n" +
			"      \"use\": \"sig\",\n" +
			"      \"kid\": \"TEST-KEY\",\n" +
			"      \"alg\": \"RS256\",\n" +
			"      \"n\": \"o7m67H_EM0qOGkLRMSTOO8gUjEq61i-hueOfR3-8gN2c9Flz4fuiSRXwbAGORbL7geNfsda9KgLSSiRFLm-67AbJ85zplLu97Jtb24TrUd4dFYBxAq4ikz7Ue0BpkKQRizUSZhePr6hkHAeOlhEQeSOUm6Urdm14fjXdJSLAd2DHSIvutuRf4XwAjhmqIo8OCoO925JX7rLDW__cVDmv5Gm7nO-q4UuLxaQnZER3smWH5myQp_25b_0nmBh8ZPFf0eG8_MOQ9GTlffwMyI8U45xFQJMKqX6_bANtt7uaoV0yTB9AosooG8cEWN3e1YyghEMu9DDwpLZtL2heMGvpXw\"\n" +
			"    }\n" +
			"  ]\n" +
			"}").getAsJsonObject();

		resource = new JsonParser().parse("{\n" +
			"  \"jwks\": " + jwks.toString() + "\n" +
			"}").getAsJsonObject();


	}

	@Test
	public void testEvaluate() {

		env.putObject("resource", resource);

		cond.evaluate(env);

		assertEquals(jwks, env.getObject("resource_jwks"));
		assertEquals(publicJwks, env.getObject("public_resource_jwks"));
	}
}
