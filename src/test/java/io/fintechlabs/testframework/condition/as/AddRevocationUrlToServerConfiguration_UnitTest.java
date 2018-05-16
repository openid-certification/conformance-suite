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
public class AddRevocationUrlToServerConfiguration_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject server;
	
	private String baseUrl;

	private AddRevocationUrlToServerConfiguration cond;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new AddRevocationUrlToServerConfiguration("UNIT-TEST", eventLog, ConditionResult.INFO);

		server = new JsonParser().parse("{\n" + 
			"}").getAsJsonObject();

		baseUrl = "https://example.com/baseurl";
		
	}

	@Test
	public void testEvaluate() {

		env.put("server", server);
		env.putString("base_url", baseUrl);

		cond.evaluate(env);
		
		assertEquals(baseUrl + "/revoke", env.getString("server", "revocation_endpoint"));
	}

	@Test
	public void testEvaluate_trailingSlash() {

		env.put("server", server);
		env.putString("base_url", baseUrl + "/");

		cond.evaluate(env);
		
		assertEquals(baseUrl + "/revoke", env.getString("server", "revocation_endpoint"));
	}

}
