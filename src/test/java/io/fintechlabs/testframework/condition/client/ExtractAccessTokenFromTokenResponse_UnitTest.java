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
public class ExtractAccessTokenFromTokenResponse_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject tokenResponse;

	private ExtractAccessTokenFromTokenResponse cond;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new ExtractAccessTokenFromTokenResponse("UNIT-TEST", eventLog, ConditionResult.INFO);

		// Example from RFC6750
		tokenResponse = new JsonParser().parse("{" +
				"\"access_token\":\"mF_9.B5f-4.1JqM\"," +
				"\"token_type\":\"Bearer\"," +
				"\"expires_in\":3600," +
				"\"refresh_token\":\"tGzv3JOkF0XG5Qx2TlKWIA\"" +
				"}").getAsJsonObject();

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.ExtractAccessTokenFromTokenResponse#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void testEvaluate_valuePresent() {

		env.put("token_endpoint_response", tokenResponse);

		cond.evaluate(env);

		verify(env, atLeastOnce()).getString("token_endpoint_response", "access_token");
		verify(env, atLeastOnce()).getString("token_endpoint_response", "token_type");

		assertThat(env.get("access_token")).isNotNull();
		assertThat(env.getString("access_token", "value")).isEqualTo(tokenResponse.get("access_token").getAsString());
		assertThat(env.getString("access_token", "type")).isEqualTo(tokenResponse.get("token_type").getAsString());

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.ExtractAccessTokenFromTokenResponse#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_valueMissing() {

		env.put("token_endpoint_response", new JsonObject());

		cond.evaluate(env);

	}

}
