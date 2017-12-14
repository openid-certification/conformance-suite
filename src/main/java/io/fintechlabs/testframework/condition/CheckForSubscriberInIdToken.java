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

import io.fintechlabs.testframework.logging.EventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class CheckForSubscriberInIdToken extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 */
	public CheckForSubscriberInIdToken(String testId, EventLog log, boolean optional) {
		super(testId, log, optional, "OB-5.2.2-8");
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = "id_token")
	public Environment evaluate(Environment env) {

		String sub = env.getString("id_token", "claims.sub");

		if (!Strings.isNullOrEmpty(sub)) {
			logSuccess("Found subscriber returned with access token", args("sub", sub));
			return env;
		} else {
			return error("Couldn't find subscriber");
		}
	}

}
