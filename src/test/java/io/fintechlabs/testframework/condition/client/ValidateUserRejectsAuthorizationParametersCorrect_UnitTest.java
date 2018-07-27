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

/**
 * @author ddrysdale
 *
 */

@RunWith(MockitoJUnitRunner.class)
public class ValidateUserRejectsAuthorizationParametersCorrect_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;
	private ValidateUserRejectsAuthorizationParametersCorrect cond;

	private String errorNoneNoOptionalFields = "{\"error\":\"access_denied\",\"state\":\"oObb8SUBuC\"}";
	private String errorNoneAllOptionalFields = "{\"error\":\"access_denied\",\"error_description\":\"incorrect credentials\",\"state\":\"oObb8SUBuC\", \"error_uri\":\"http://anerror.com\"}";
	private String errorTooManyFields = "{\"error\":\"access_denied\",\"error_description\":\"incorrect credentials\",\"state\":\"oObb8SUBuC\", \"error_uri\":\"http://anerror.com\",\"extraField\":\"Illegal Extra Field\"}";
	private String errorErrorMissing = "{\"error_description\":\"incorrect credentials\",\"state\":\"oObb8SUBuC\", \"error_uri\":\"http://anerror.com\"}";
	private String errorStateMissing = "{\"error\":\"access_denied\",\"error_description\":\"incorrect credentials\", \"error_uri\":\"http://anerror.com\"}";

	private String stateGood = "oObb8SUBuC";
	private String stateWrong = "BrokenDunk";

	/*
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new ValidateUserRejectsAuthorizationParametersCorrect("UNIT-TEST", eventLog, ConditionResult.INFO);

	}

	private void addState(String globalStateToAdd) {
		env.putString("state", globalStateToAdd);
	}

	private void doTestString(String stringToTest) {
		JsonObject jsonErrorNoneNoOptionalFields = new JsonParser().parse(stringToTest).getAsJsonObject();
		env.put("callback_params", jsonErrorNoneNoOptionalFields);
		cond.evaluate(env);
	}

	/**
	 * Test method for
	 * {@link io.fintechlabs.testframework.condition.client.ValidateUserRejectsAuthorizationParametersCorrect#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void testEvaluate_noErrorNoOptionalFieldsGoodState() {
		addState(stateGood);
		doTestString(errorNoneNoOptionalFields);
	}

	/**
	 * Test method for
	 * {@link io.fintechlabs.testframework.condition.client.ValidateUserRejectsAuthorizationParametersCorrect#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void testEvaluate_noErrorAllOptionalFieldsGoodState() {
		addState(stateGood);
		doTestString(errorNoneAllOptionalFields);
	}

	/**
	 * Test method for
	 * {@link io.fintechlabs.testframework.condition.client.ValidateUserRejectsAuthorizationParametersCorrect#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_noErrorNoOptionalFieldsWrongState() {
		addState(stateWrong);
		doTestString(errorNoneNoOptionalFields);
	}

	/**
	 * Test method for
	 * {@link io.fintechlabs.testframework.condition.client.ValidateUserRejectsAuthorizationParametersCorrect#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_noErrorAllOptionalFieldsWrongState() {
		addState(stateWrong);
		doTestString(errorNoneAllOptionalFields);
	}

	/**
	 * Test method for
	 * {@link io.fintechlabs.testframework.condition.client.ValidateUserRejectsAuthorizationParametersCorrect#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_errorMissing() {
		doTestString(errorErrorMissing);
	}

	/**
	 * Test method for
	 * {@link io.fintechlabs.testframework.condition.client.ValidateUserRejectsAuthorizationParametersCorrect#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_stateMissing() {
		doTestString(errorStateMissing);
	}

	/**
	 * Test method for
	 * {@link io.fintechlabs.testframework.condition.client.ValidateUserRejectsAuthorizationParametersCorrect#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_errorTooManyFields() {
		doTestString(errorTooManyFields);
	}
}
