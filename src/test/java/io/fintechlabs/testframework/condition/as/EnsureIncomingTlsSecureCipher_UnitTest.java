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

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class EnsureIncomingTlsSecureCipher_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private EnsureIncomingTlsSecureCipher cond;

	private List<JsonObject> hasTls;
	private JsonObject wrongTls;
	private JsonObject missingTls;
	private JsonObject onlyTls;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new EnsureIncomingTlsSecureCipher("UNIT-TEST", eventLog, ConditionResult.INFO);

		hasTls = new ArrayList<>();

		hasTls.add(new JsonParser().parse("{\"headers\": "
			+ "{\"X-Ssl-Protocol\": \"TLSv1.2\", \"X-Ssl-Cipher\": \"DHE-RSA-AES128-GCM-SHA256\"}"
			+ "}").getAsJsonObject());
		hasTls.add(new JsonParser().parse("{\"headers\": "
			+ "{\"X-Ssl-Protocol\": \"TLSv1.2\", \"X-Ssl-Cipher\": \"ECDHE-RSA-AES128-GCM-SHA256\"}"
			+ "}").getAsJsonObject());
		hasTls.add(new JsonParser().parse("{\"headers\": "
			+ "{\"X-Ssl-Protocol\": \"TLSv1.2\", \"X-Ssl-Cipher\": \"DHE-RSA-AES256-GCM-SHA384\"}"
			+ "}").getAsJsonObject());
		hasTls.add(new JsonParser().parse("{\"headers\": "
			+ "{\"X-Ssl-Protocol\": \"TLSv1.2\", \"X-Ssl-Cipher\": \"ECDHE-RSA-AES256-GCM-SHA384\"}"
			+ "}").getAsJsonObject());
		wrongTls = new JsonParser().parse("{\"headers\": "
			+ "{\"X-Ssl-Protocol\": \"TLSv1.2\", \"X-Ssl-Cipher\": \"DUCK-TAPE-AND-A-PRAYER\"}"
			+ "}").getAsJsonObject();
		onlyTls = new JsonParser().parse("{\"headers\": "
			+ "{\"X-Ssl-Cipher\": \"ECDHE-RSA-AES128-GCM-SHA256\"}"
			+ "}").getAsJsonObject();
		missingTls = new JsonParser().parse("{\"headers\": "
			+ "{\"X-Ssl-Protocol\": \"TLSv1.2\"}"
			+ "}").getAsJsonObject();

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.as.EnsureClientCertificateCNMatchesClientId#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void testEvaluate_noError() {

		for (JsonObject tls : hasTls) {
			env.putObject("client_request", tls);

			cond.evaluate(env);

			verify(env, atLeastOnce()).getString("client_request", "headers.X-Ssl-Cipher");
		}

	}
	@Test(expected = ConditionError.class)
	public void testEvaluate_wrong() {

		env.putObject("client_request", wrongTls);

		cond.evaluate(env);

	}
	@Test(expected = ConditionError.class)
	public void testEvaluate_missing() {

		env.putObject("client_request", missingTls);

		cond.evaluate(env);

	}
	@Test
	public void testEvaluate_only() {

		env.putObject("client_request", onlyTls);

		cond.evaluate(env);

		verify(env, atLeastOnce()).getString("client_request", "headers.X-Ssl-Cipher");

	}
}
