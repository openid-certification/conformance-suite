/*******************************************************************************
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

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
import io.fintechlabs.testframework.condition.client.ExtractJWKsFromClientConfiguration;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class ExtractJWKsFromClientConfiguration_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject client;

	private JsonObject jwks;

	private JsonObject publicJwks;

	private ExtractJWKsFromClientConfiguration cond;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new ExtractJWKsFromClientConfiguration("UNIT-TEST", eventLog, ConditionResult.INFO);

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

		client = new JsonParser().parse("{\n" +
			"  \"jwks\": " + jwks.toString() + "\n" +
			"}").getAsJsonObject();


	}

	@Test
	public void testEvaluate() {

		env.put("client", client);

		cond.evaluate(env);

		assertEquals(jwks, env.getObject("client_jwks"));
		assertEquals(publicJwks, env.getObject("public_client_jwks"));
	}
}
