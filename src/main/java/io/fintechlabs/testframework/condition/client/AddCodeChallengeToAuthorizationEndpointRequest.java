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
import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author srmoore
 *
 */
public class AddCodeChallengeToAuthorizationEndpointRequest extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 * @param optional
	 */
	public AddCodeChallengeToAuthorizationEndpointRequest(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(strings = {"code_challenge","code_challenge_method"}, required = "authorization_endpoint_request")
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {
		String code_challenge = env.getString("code_challenge");
		if (Strings.isNullOrEmpty(code_challenge)) {
			throw error("Couldn't find code_challenge value");
		}

		String code_challenge_method = env.getString("code_challenge_method");
		if (Strings.isNullOrEmpty(code_challenge)) {
			throw error("Couldn't find code_challenge_method value");
		}

		if (!env.containsObject("authorization_endpoint_request")) {
			throw error("Couldn't find authorization endpoint request");
		}

		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");

		authorizationEndpointRequest.addProperty("code_challenge", code_challenge);
		authorizationEndpointRequest.addProperty("code_challenge_method", code_challenge_method);

		env.put("authorization_endpoint_request", authorizationEndpointRequest);

		logSuccess("Added code_challenge and code_challenge_method parameters to request", authorizationEndpointRequest);

		return env;

	}

}
