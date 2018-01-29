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

package io.fintechlabs.testframework.condition.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.time.Instant;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class CreateClientAuthenticationAssertionClaims_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CreateClientAuthenticationAssertionClaims cond;

	private String clientId = "client";

	private String tokenEndpoint = "https://server.example.com/token";

	private JsonObject client;

	private JsonObject server;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		client = new JsonObject();
		client.addProperty("client_id", clientId);

		server = new JsonObject();
		server.addProperty("token_endpoint", tokenEndpoint);

		cond = new CreateClientAuthenticationAssertionClaims("UNIT-TEST", eventLog, ConditionResult.INFO);

	}

	@Test
	public void testEvaluate() {

		env.put("client", client);
		env.put("server", server);

		cond.evaluate(env);

		assertThat(env.get("client_assertion_claims")).isNotNull();

		JsonObject claims = env.get("client_assertion_claims");

		assertThat(claims.get("iss").getAsString()).isEqualTo(clientId);
		assertThat(claims.get("sub").getAsString()).isEqualTo(clientId);
		assertThat(claims.get("aud").getAsString()).isEqualTo(tokenEndpoint);

		assertThat(claims.get("jti")).isNotNull();

		Instant now = Instant.now();

		assertThat(claims.get("iat").getAsLong()).isCloseTo(now.getEpochSecond(), within(5L)); // five second leeway
		assertThat(claims.get("exp").getAsLong()).isCloseTo(now.plusSeconds(60).getEpochSecond(), within(5L)); // 60 seconds in the future, 5 second leeway

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_missingClient() {
		env.put("server", server);

		cond.evaluate(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_missingServer() {
		env.put("client", client);

		cond.evaluate(env);
	}

}
