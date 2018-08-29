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

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class EnsureBearerAccessTokenNotInParams_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private EnsureBearerAccessTokenNotInParams cond;

	private JsonObject hasToken;
	private JsonObject missingToken;
	private JsonObject missingParams;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new EnsureBearerAccessTokenNotInParams("UNIT-TEST", eventLog, ConditionResult.INFO);

		hasToken = new JsonParser().parse(
			"{\"params\": " +
				"{\"access_token\": \"foo123456\"}" +
			"}").getAsJsonObject();
		missingToken = new JsonParser().parse(
			"{\"params\": " +
				"{}" +
			"}").getAsJsonObject();
		missingParams = new JsonParser().parse(
			"{}").getAsJsonObject();

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.as.EnsureClientCertificateCNMatchesClientId#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_hasToken() {

		env.putObject("incoming_request", hasToken);

		cond.evaluate(env);


	}
	@Test
	public void testEvaluate_missingToken() {

		env.putObject("incoming_request", missingToken);

		cond.evaluate(env);

		verify(env, atLeastOnce()).getString("incoming_request", "params.access_token");

	}
	@Test
	public void testEvaluate_missing() {

		env.putObject("incoming_request", missingParams);

		cond.evaluate(env);

		verify(env, atLeastOnce()).getString("incoming_request", "params.access_token");

	}
}
