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
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class CreateTokenEndpointResponse extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 * @param optional
	 */
	public CreateTokenEndpointResponse(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(strings = {"access_token", "token_type"}) // note the others are optional
	@PostEnvironment(required = "token_endpoint_response")
	public Environment evaluate(Environment env) {
		
		String accessToken = env.getString("access_token");
		String tokenType = env.getString("token_type");
		String idToken = env.getString("id_token");
		String refreshToken = env.getString("refresh_token");
		String scope = env.getString("scope");

		if (Strings.isNullOrEmpty(accessToken) || Strings.isNullOrEmpty(tokenType)) {
			return error("Missing required access_token or token_type");
		}
		
		JsonObject tokenEndpointResponse = new JsonObject();

		tokenEndpointResponse.addProperty("access_token", accessToken);
		tokenEndpointResponse.addProperty("token_type", tokenType);
		
		if (!Strings.isNullOrEmpty(idToken)) {
			tokenEndpointResponse.addProperty("id_token", idToken);
		}
		
		if (!Strings.isNullOrEmpty(refreshToken)) {
			tokenEndpointResponse.addProperty("refresh_token", refreshToken);
		}
		
		if (!Strings.isNullOrEmpty(scope)) {
			tokenEndpointResponse.addProperty("scope", scope);
		}
		
		env.put("token_endpoint_response", tokenEndpointResponse);
		
		logSuccess("Created token endpoint response", tokenEndpointResponse);
		
		return env;
		
	}

}
