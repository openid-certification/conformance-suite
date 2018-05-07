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

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class CreateTokenEndpointRequestForClientCredentialsGrant_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CreateTokenEndpointRequestForClientCredentialsGrant cond;
	
	private JsonObject clientWithScope;
	
	private JsonObject clientWithoutScope;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new CreateTokenEndpointRequestForClientCredentialsGrant("UNIT-TEST", eventLog, ConditionResult.INFO);
		
		clientWithScope = new JsonParser().parse("{\"scope\": \"foo bar\"}").getAsJsonObject();
		clientWithoutScope = new JsonParser().parse("{}").getAsJsonObject();
		
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.CreateTokenEndpointRequestForClientCredentialsGrant#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void testEvaluate_withScope() {

		env.put("client", clientWithScope);
		
		cond.evaluate(env);

		JsonObject parameters = env.get("token_endpoint_request_form_parameters");

		assertThat(parameters).isNotNull();
		assertThat(parameters.get("grant_type").getAsString()).isEqualTo("client_credentials");

		assertThat(parameters.has("scope")).isTrue();
		assertThat(parameters.get("scope")).isEqualTo(clientWithScope.get("scope"));
	}

	@Test
	public void testEvaluate_withoutScope() {

		env.put("client", clientWithoutScope);
		
		cond.evaluate(env);

		JsonObject parameters = env.get("token_endpoint_request_form_parameters");

		assertThat(parameters).isNotNull();
		assertThat(parameters.get("grant_type").getAsString()).isEqualTo("client_credentials");
		
		assertThat(parameters.has("scope")).isFalse();
	}
}
