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

import com.google.common.base.Strings;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class EnsureMatchingFAPIInteractionId extends AbstractCondition {

	public EnsureMatchingFAPIInteractionId(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = "resource_endpoint_response_headers", strings = "fapi_interaction_id")
	public Environment evaluate(Environment env) {

		// get the client ID from the configuration
		String expected = env.getString("fapi_interaction_id");
		String actual = env.getString("resource_endpoint_response_headers", "x-fapi-interaction-id");

		if (!Strings.isNullOrEmpty(expected) && expected.equals(actual)) {
			logSuccess("Interaction ID matched", args("fapi_interaction_id", Strings.nullToEmpty(actual)));
			return env;
		} else {
			throw error("Mismatch between interaction IDs", args("expected", Strings.nullToEmpty(expected), "actual", Strings.nullToEmpty(actual)));
		}

	}

}
