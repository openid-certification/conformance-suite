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
import com.google.gson.JsonParser;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class GetResourceEndpointConfiguration_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private static JsonObject resourceConfig = new JsonParser().parse("{" +
			"\"resourceUrl\":\"https://example.com/\"" +
			"}").getAsJsonObject();

	private GetResourceEndpointConfiguration cond;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new GetResourceEndpointConfiguration("UNIT-TEST", eventLog, ConditionResult.INFO);

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.GetResourceEndpointConfiguration#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void testEvaluate_valuePresent() {

		JsonObject config = new JsonObject();
		config.add("resource", resourceConfig);
		env.put("config", config);

		cond.evaluate(env);

		verify(env, atLeastOnce()).findElement("config", "resource");
		assertThat(env.get("resource")).isEqualTo(resourceConfig);
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.GetResourceEndpointConfiguration#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_valueMissing() {

		env.put("config", new JsonObject());

		cond.evaluate(env);
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.GetResourceEndpointConfiguration#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_configMissing() {

		cond.evaluate(env);
	}

}
