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

import java.util.Date;

import org.apache.http.client.utils.DateUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.client.CheckForDateHeaderInResourceResponse;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class CheckForDateHeaderInResourceResponse_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CheckForDateHeaderInResourceResponse cond;

	@Before
	public void setUp() throws Exception {
		cond = new CheckForDateHeaderInResourceResponse("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.CheckForDateHeaderInResourceResponse#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void testEvaluate_noError() {

		JsonObject headers = new JsonObject();
		headers.addProperty("Date", DateUtils.formatDate(new Date()));
		env.put("resource_endpoint_response_headers", headers);

		cond.evaluate(env);

		verify(env, atLeastOnce()).getString("resource_endpoint_response_headers", "Date");
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.CheckForDateHeaderInResourceResponse#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_oldDate() {

		// Obviously, don't run this test on 6 Nov 1994 ;)

		JsonObject headers = new JsonObject();
		headers.addProperty("Date", "Sun, 06 Nov 1994 08:49:37 GMT"); // Example from RFC 7231
		env.put("resource_endpoint_response_headers", headers);

		cond.evaluate(env);

		verify(env, atLeastOnce()).getString("resource_endpoint_response_headers", "Date");
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.CheckForDateHeaderInResourceResponse#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_invalidDate() {

		JsonObject headers = new JsonObject();
		headers.addProperty("Date", "this is not a date");
		env.put("resource_endpoint_response_headers", headers);

		cond.evaluate(env);
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.CheckForDateHeaderInResourceResponse#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingDate() {

		JsonObject headers = new JsonObject();
		env.put("resource_endpoint_response_headers", headers);

		cond.evaluate(env);
	}

}
