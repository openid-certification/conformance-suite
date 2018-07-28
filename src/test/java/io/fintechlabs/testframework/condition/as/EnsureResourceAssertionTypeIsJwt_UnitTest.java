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

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class EnsureResourceAssertionTypeIsJwt_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject resourceAssertion;

	private String assertionType;

	private EnsureResourceAssertionTypeIsJwt cond;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new EnsureResourceAssertionTypeIsJwt("UNIT-TEST", eventLog, ConditionResult.INFO);

		assertionType = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";

		resourceAssertion = new JsonParser().parse("{\n" +
				"\"assertion_type\": \"" + assertionType + "\"" +
			"}").getAsJsonObject();


	}

	@Test
	public void testEvaluate() {
		env.put("resource_assertion", resourceAssertion);

		cond.evaluate(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_noAssertionType() {

		resourceAssertion.remove("assertion_type");

		env.put("resource_assertion", resourceAssertion);

		cond.evaluate(env);

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_blankAssertionType() {

		resourceAssertion.addProperty("assertion_type", "");

		env.put("resource_assertion", resourceAssertion);

		cond.evaluate(env);

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_nullAssertionType() {

		resourceAssertion.add("assertion_type", JsonNull.INSTANCE);

		env.put("resource_assertion", resourceAssertion);

		cond.evaluate(env);

	}

}
