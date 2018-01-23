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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class AddAccountRequestIdToAuthorizationEndpointRequest extends AbstractCondition {

	public AddAccountRequestIdToAuthorizationEndpointRequest(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(strings = "account_request_id", required = "authorization_endpoint_request")
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {

		JsonObject authorizationEndpointRequest = env.get("authorization_endpoint_request");

		JsonObject claims;
		if (authorizationEndpointRequest.has("claims")) {
			JsonElement claimsElement = authorizationEndpointRequest.get("claims");
			if (claimsElement.isJsonObject()) {
				claims = claimsElement.getAsJsonObject();
			} else {
				return error("Invalid claims in request", args("authorization_endpoint_request", authorizationEndpointRequest));
			}
		} else {
			claims = new JsonObject();
			authorizationEndpointRequest.add("claims", claims);
		}

		JsonObject claimsIdToken;
		if (claims.has("id_token")) {
			JsonElement idTokenElement = claims.get("id_token");
			if (idTokenElement.isJsonObject()) {
				claimsIdToken = idTokenElement.getAsJsonObject();
			} else {
				return error("Invalid id_token in request claims", args("authorization_endpoint_request", authorizationEndpointRequest));
			}
		} else {
			claimsIdToken = new JsonObject();
			claims.add("id_token", claimsIdToken);
		}

		JsonObject intentId = new JsonObject();
		intentId.addProperty("value", env.getString("account_request_id"));
		intentId.addProperty("essential", true);
		claimsIdToken.add("openbanking_intent_id", intentId);

		env.put("authorization_endpoint_request", authorizationEndpointRequest);

		logSuccess("Added account request ID to request", args("authorization_endpoint_request", authorizationEndpointRequest));

		return env;
	}

}
