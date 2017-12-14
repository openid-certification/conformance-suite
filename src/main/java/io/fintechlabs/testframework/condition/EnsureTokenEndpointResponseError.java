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

public class EnsureTokenEndpointResponseError extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 */
	public EnsureTokenEndpointResponseError(String testId, EventLog log, boolean optional) {
		super(testId, log, optional);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = "token_endpoint_response")
	public Environment evaluate(Environment env) {

		if (!env.containsObj("token_endpoint_response")) {
			return error("Couldn't find token endpoint response");
		}

		if (!Strings.isNullOrEmpty(env.getString("token_endpoint_response", "error"))) {
			logSuccess("Found error in token endpoint error response", env.get("token_endpoint_response"));
			return env;
		} else {
			return error("No error from token endpoint");
		}

	}

}
