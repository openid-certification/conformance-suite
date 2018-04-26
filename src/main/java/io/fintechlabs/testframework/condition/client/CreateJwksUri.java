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
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class CreateJwksUri extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 * @param conditionResultOnFailure
	 * @param requirements
	 */
	public CreateJwksUri(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(strings = "base_url")
	@PostEnvironment(strings = "jwks_uri")
	public Environment evaluate(Environment env) {
		String baseUrl = env.getString("base_url");

		if (Strings.isNullOrEmpty(baseUrl)) {
			throw error("Base URL was null or empty");
		}

		// calculate the redirect URI based on our given base URL
		String jwksUri = baseUrl + "/jwks";
		env.putString("jwks_uri", jwksUri);

		logSuccess("Created JWKs URI",
			args("jwks_uri", jwksUri));

		return env;
	}

}
