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
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;


public class ExtractExpiresInFromTokenEndpointResponse extends AbstractCondition {
	
	public ExtractExpiresInFromTokenEndpointResponse(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}
	
	@Override
	@PreEnvironment(required = "token_endpoint_response")
	@PostEnvironment(required = "expires_in")
	public Environment evaluate(Environment env) {
		
		JsonObject tokenEndpoint = env.get("token_endpoint_response");
		if (tokenEndpoint == null) {
			return error("Couldn't find token_endpoint for expires_in analysis.");
		}
		
		JsonElement expiresInValue = tokenEndpoint.get("expires_in");
		if (expiresInValue == null) {
			tokenEndpoint.addProperty("result",ConditionResult.REVIEW.toString());
			log("expires_in: UNDEFINED.", tokenEndpoint);
			return env;
		}
		
		/* Create our cut down JsonObject with just a single value in it */
		JsonObject value = new JsonObject(); 
		value.add("expires_in", expiresInValue);
		env.put("expires_in", value);
		
		logSuccess("expires_in: DEFINED", value);

		return env;
		
	}
}
