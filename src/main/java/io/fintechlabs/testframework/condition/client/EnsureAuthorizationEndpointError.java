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

/**
 * Ensure there was an error from the authorization endpoint. If there isn't, log it and quit.
 *
 * @author srmoore
 *
 */
public class EnsureAuthorizationEndpointError extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 */
	public EnsureAuthorizationEndpointError(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = "callback_params")
	public Environment evaluate(Environment in) {
		if (!in.containsObject("callback_params")) {
			throw error("Couldn't find callback parameters");
		}

		if (!Strings.isNullOrEmpty(in.getString("callback_params", "error"))) {
			logSuccess("Error from the authorization endpoint", in.getObject("callback_params"));
			return in;
		} else {
			throw error("No error from authorization endpoint");
		}

	}

}
