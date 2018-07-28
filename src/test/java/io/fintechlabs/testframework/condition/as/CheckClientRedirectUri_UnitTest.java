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

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class CheckClientRedirectUri_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject client;

	private CheckClientRedirectUri cond;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new CheckClientRedirectUri("UNIT-TEST", eventLog, ConditionResult.INFO);

		client = new JsonParser().parse("{\n" +
			"  \"redirect_uris\": "
			+ "[\"http://localhost/foo\"]\n" +
			"}").getAsJsonObject();

	}

	@Test
	public void testEvaluate_httpLocalhost() {

		client = new JsonParser().parse("{\n" +
			"  \"redirect_uris\": "
			+ "[\"http://localhost/foo\"]\n" +
			"}").getAsJsonObject();

		env.put("client", client);

		cond.evaluate(env);
	}

	@Test
	public void testEvaluate_httpLocalIp() {

		client = new JsonParser().parse("{\n" +
			"  \"redirect_uris\": "
			+ "[\"http://127.0.0.1/foo\"]\n" +
			"}").getAsJsonObject();

		env.put("client", client);

		cond.evaluate(env);
	}

	@Test
	public void testEvaluate_httpLocalIpv6() {

		client = new JsonParser().parse("{\n" +
			"  \"redirect_uris\": "
			+ "[\"http://::::1/foo\"]\n" +
			"}").getAsJsonObject();

		env.put("client", client);

		cond.evaluate(env);
	}

	@Test
	public void testEvaluate_httpsNonLocalhost() {

		client = new JsonParser().parse("{\n" +
			"  \"redirect_uris\": "
			+ "[\"https://example.com/foo\"]\n" +
			"}").getAsJsonObject();

		env.put("client", client);

		cond.evaluate(env);
	}

	@Test
	public void testEvaluate_nonHttp() {

		client = new JsonParser().parse("{\n" +
			"  \"redirect_uris\": "
			+ "[\"nonhttp:/callback\"]\n" +
			"}").getAsJsonObject();

		env.put("client", client);

		cond.evaluate(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_httpNonLocalhost() {

		client = new JsonParser().parse("{\n" +
			"  \"redirect_uris\": "
			+ "[\"http://example.com/foo\"]\n" +
			"}").getAsJsonObject();

		env.put("client", client);

		cond.evaluate(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_valueMissing() {

		cond.evaluate(env);

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_empty() {

		client = new JsonParser().parse("{\n" +
			"  \"redirect_uris\": "
			+ "[]\n" +
			"}").getAsJsonObject();

		env.put("client", client);

		cond.evaluate(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_null() {

		client = new JsonParser().parse("{}").getAsJsonObject();

		env.put("client", client);

		cond.evaluate(env);
	}

}
