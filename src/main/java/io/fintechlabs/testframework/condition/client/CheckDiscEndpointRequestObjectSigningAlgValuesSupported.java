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

//Author: ddrysdale

package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class CheckDiscEndpointRequestObjectSigningAlgValuesSupported extends ValidateJsonArray {

	private static final String environmentVariable = "request_object_signing_alg_values_supported";

	private static final String[] SET_VALUES = new String[] { "RS256", "PS256" };

	private static final String errorMessageWhenNull = "Endpoint Request Object Signing Alg Values: Not Found";
	private static final String errorMessageNotEnough = "No matching value from server";


	public CheckDiscEndpointRequestObjectSigningAlgValuesSupported(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		return validate(env, environmentVariable, SET_VALUES, 1,
				errorMessageWhenNull, errorMessageNotEnough);
	}

}
