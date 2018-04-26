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
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class AuthenticateClientWithClientSecret extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 * @param optional
	 */
	public AuthenticateClientWithClientSecret(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = "client_authentication")
	@PostEnvironment(required = "client_authentication_success")
	public Environment evaluate(Environment env) {
		if (env.containsObj("client_authentication_success")) {
			throw error("Found existing client authentication");
		}

		if (!env.containsObj("client_authentication")) {
			throw error("Couldn't find client authentication");
		}

		if (Strings.isNullOrEmpty(env.getString("client_authentication", "method"))) {
			throw error("Couldn't determine client authentication method");
		}

		if (env.getString("client_authentication", "method").equals("client_secret_post")
			|| env.getString("client_authentication", "method").equals("client_secret_basic")) {

			String expected = env.getString("client", "client_secret");
			String actual = env.getString("client_authentication", "client_secret");

			if (!Strings.isNullOrEmpty(expected)
				&& expected.equals(actual)) {

				logSuccess("Authenticated the client", args("client_authentication_success", env.getString("client_authentication", "method")));

				env.putString("client_authentication_success", env.getString("client_authentication", "method"));

				return env;

			} else {
				throw error("Mismatch client secrets", args("expected", expected, "actual", actual));
			}

		} else {
			throw error("Can't handle client method " + env.getString("client_authentication", "method"));
		}

	}

}
