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

import com.google.common.base.Strings;

import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class RequireBearerAccessToken extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 * @param optional
	 */
	public RequireBearerAccessToken(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(strings = {"incoming_access_token", "access_token"})
	public Environment evaluate(Environment env) {

		String actual = env.getString("incoming_access_token");
		String expected = env.getString("access_token");
		
		if (!Strings.isNullOrEmpty(actual) && actual.equals(expected)) {
			logSuccess("Found access token in request", args("actual", Strings.nullToEmpty(actual)));
			return env;
		} else {
			return error("Invalid access token ", args("expected", Strings.nullToEmpty(expected), "actual", Strings.nullToEmpty(actual)));
		}
		
	}

}
