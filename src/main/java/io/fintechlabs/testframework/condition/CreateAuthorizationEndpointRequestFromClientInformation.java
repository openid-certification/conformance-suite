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
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.logging.EventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class CreateAuthorizationEndpointRequestFromClientInformation extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 * @param optional
	 */
	public CreateAuthorizationEndpointRequestFromClientInformation(String testId, EventLog log, boolean optional) {
		super(testId, log, optional);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	public Environment evaluate(Environment env) {

		if (!env.containsObj("client")) {
			return error("Couldn't find client configuration");
		}
		
		String clientId = env.getString("client_id");
		
		if (Strings.isNullOrEmpty(clientId)) {
			return error("Couldn't find client ID");
		}
		
		String redirectUri = env.getString("redirect_uri");
		
		if (Strings.isNullOrEmpty(redirectUri)) {
			return error("Couldn't find redirect URI");
		}
		
		JsonObject authorizationEndpointRequest = new JsonObject();
		
		authorizationEndpointRequest.addProperty("client_id", clientId);
		authorizationEndpointRequest.addProperty("redirect_uri", redirectUri);
		
		String scope = env.getString("client", "scope");
		if (!Strings.isNullOrEmpty(scope)) {
			authorizationEndpointRequest.addProperty("scope", scope);
		} else {
			log("Leaving off 'scope' parameter from authorization request");
		}
		
		env.put("authorization_endpoint_request", authorizationEndpointRequest);
		
		logSuccess("Created authorization endpoint request", authorizationEndpointRequest);
		
		return env;

	}

}
