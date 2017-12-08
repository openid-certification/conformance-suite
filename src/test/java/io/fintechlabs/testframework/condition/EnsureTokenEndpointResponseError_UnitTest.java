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

package io.fintechlabs.testframework.condition;

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

import io.fintechlabs.testframework.logging.EventLog;
import io.fintechlabs.testframework.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class EnsureTokenEndpointResponseError_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private EventLog eventLog;

	private JsonObject successResponse;

	private JsonObject errorResponse;

	private EnsureTokenEndpointResponseError cond;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new EnsureTokenEndpointResponseError("UNIT-TEST", eventLog, false);

		successResponse = new JsonParser().parse("{"
				+ "\"accessToken\":\"2YotnFZFEjr1zCsicMWpAA\","
				+ "\"token_type\":\"example\","
				+ "\"expires_in\":3600,"
				+ "\"refresh_token\":\"tGzv3JOkF0XG5Qx2TlKWIA\","
				+ "\"example_parameter\":\"example_value\""
				+ "}").getAsJsonObject();

		errorResponse = new JsonParser().parse("{"
				+ "\"error\":\"invalid_request\""
				+ "}").getAsJsonObject();
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.EnsureTokenEndpointResponseError#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void testEvaluate_errorInResponse() {

		env.put("token_endpoint_response", errorResponse);

		cond.evaluate(env);

		verify(env, atLeastOnce()).getString("token_endpoint_response", "error");
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.EnsureTokenEndpointResponseError#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_noErrorInResponse() {

		env.put("token_endpoint_response", successResponse);

		cond.evaluate(env);
	}

}
