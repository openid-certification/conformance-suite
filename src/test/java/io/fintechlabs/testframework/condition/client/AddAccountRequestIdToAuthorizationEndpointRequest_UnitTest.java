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
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.client.AddAccountRequestIdToAuthorizationEndpointRequest;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class AddAccountRequestIdToAuthorizationEndpointRequest_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private AddAccountRequestIdToAuthorizationEndpointRequest cond;

	private String requestId;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new AddAccountRequestIdToAuthorizationEndpointRequest("UNIT-TEST", eventLog, ConditionResult.INFO);

		requestId = "88379"; // OpenBanking example
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.AddAccountRequestIdToAuthorizationEndpointRequest#evaluate(io.fintechlabs.testframework.testmodule.Environment).
	 */
	@Test
	public void testEvaluate() {

		env.put("authorization_endpoint_request", new JsonObject());
		env.putString("account_request_id", requestId);

		cond.evaluate(env);

		verify(env, atLeastOnce()).getString("account_request_id");

		assertThat(env.getString("authorization_endpoint_request", "claims.id_token.openbanking_intent_id.value")).isEqualTo(requestId);
		assertThat(env.findElement("authorization_endpoint_request", "claims.id_token.openbanking_intent_id.essential").getAsBoolean()).isTrue();

	}

}
