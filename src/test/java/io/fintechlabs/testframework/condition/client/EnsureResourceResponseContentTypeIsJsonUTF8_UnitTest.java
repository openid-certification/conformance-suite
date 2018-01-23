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

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.client.EnsureResourceResponseContentTypeIsJsonUTF8;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class EnsureResourceResponseContentTypeIsJsonUTF8_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private EnsureResourceResponseContentTypeIsJsonUTF8 cond;

	@Before
	public void setUp() throws Exception {
		cond = new EnsureResourceResponseContentTypeIsJsonUTF8("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.EnsureResourceResponseContentTypeIsJsonUTF8#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void testEvaluate_noError() {

		JsonObject headers = new JsonObject();
		headers.addProperty("Content-Type", "application/json; charset=UTF-8");
		env.put("resource_endpoint_response_headers", headers);

		cond.evaluate(env);

		verify(env, atLeastOnce()).getString("resource_endpoint_response_headers", "Content-Type");
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.EnsureResourceResponseContentTypeIsJsonUTF8#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_invalidCharset() {

		JsonObject headers = new JsonObject();
		headers.addProperty("Content-Type", "application/json; charset=Shift_JIS");
		env.put("resource_endpoint_response_headers", headers);

		cond.evaluate(env);
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.EnsureResourceResponseContentTypeIsJsonUTF8#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingCharset() {

		JsonObject headers = new JsonObject();
		headers.addProperty("Content-Type", "application/json");
		env.put("resource_endpoint_response_headers", headers);

		cond.evaluate(env);
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.EnsureResourceResponseContentTypeIsJsonUTF8#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_invalidType() {

		JsonObject headers = new JsonObject();
		headers.addProperty("Content-Type", "text/json; charset=UTF-8");
		env.put("resource_endpoint_response_headers", headers);

		cond.evaluate(env);
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.EnsureResourceResponseContentTypeIsJsonUTF8#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingContentType() {

		JsonObject headers = new JsonObject();
		env.put("resource_endpoint_response_headers", headers);

		cond.evaluate(env);
	}

}
