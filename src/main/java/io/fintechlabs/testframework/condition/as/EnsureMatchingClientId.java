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

import com.google.common.base.Strings;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class EnsureMatchingClientId extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 * @param optional
	 */
	public EnsureMatchingClientId(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = {"client", "authorization_endpoint_request"})
	public Environment evaluate(Environment env) {

		// get the client ID from the configuration
		String expected = env.getString("client", "client_id");
		String actual = env.getString("authorization_endpoint_request", "client_id");
		
		if (!Strings.isNullOrEmpty(expected) && expected.equals(actual)) {
			logSuccess("Client ID matched",
					args("client_id", Strings.nullToEmpty(actual)));
			return env;
		} else {
			return error("Mismatch between client ID", args("expected", Strings.nullToEmpty(expected), "actual", Strings.nullToEmpty(actual)));
		}

		
	}

}
