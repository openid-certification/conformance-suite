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
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class CopyAccessTokenFromASToClient_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject accessToken;

	private String accessTokenValue;

	private String tokenType;

	private CopyAccessTokenFromASToClient cond;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new CopyAccessTokenFromASToClient("UNIT-TEST", eventLog, ConditionResult.INFO);

		accessTokenValue = "foo1234556";
		tokenType = "Bearer";

		accessToken = new JsonParser().parse("{\n" +
				"\"type\": \"" + tokenType + "\",\n" +
				"\"value\": \"" + accessTokenValue + "\"\n" +
			"}").getAsJsonObject();

	}

	@Test
	public void testEvaluate() {

		env.putString("access_token", accessTokenValue);
		env.putString("token_type", tokenType);

		cond.evaluate(env);

		JsonObject res = env.get("access_token");

		assertEquals(accessToken, res);

	}


}
