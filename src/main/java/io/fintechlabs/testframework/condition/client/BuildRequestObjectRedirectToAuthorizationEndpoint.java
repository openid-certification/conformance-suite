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

import java.util.Arrays;
import java.util.List;

import org.springframework.web.util.UriComponentsBuilder;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class BuildRequestObjectRedirectToAuthorizationEndpoint extends AbstractCondition {

	private static final List<String> REQUIRED_PARAMETERS = Arrays.asList(new String[] {
		"response_type",
		"client_id",
		"scope",
		"redirect_uri"
	});

	/**
	 * @param testId
	 * @param log
	 */
	public BuildRequestObjectRedirectToAuthorizationEndpoint(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = {"authorization_endpoint_request", "request_object_claims", "server"}, strings = "request_object")
	@PostEnvironment(strings = "redirect_to_authorization_endpoint")
	public Environment evaluate(Environment env) {

		JsonObject authorizationEndpointRequest = env.get("authorization_endpoint_request");
		if (authorizationEndpointRequest == null) {
			return error("Couldn't find authorization endpoint request");
		}

		String requestObject = env.getString("request_object");
		if (requestObject == null) {
			return error("Couldn't find request object");
		}

		JsonObject requestObjectClaims = env.get("request_object_claims");
		if (requestObjectClaims == null) {
			return error("Couldn't find request object claims");
		}

		String authorizationEndpoint = env.getString("server", "authorization_endpoint");
		if (Strings.isNullOrEmpty(authorizationEndpoint)) {
			return error("Couldn't find authorization endpoint");
		}

		// send a front channel request to start things off
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(authorizationEndpoint);

		builder.queryParam("request", requestObject);

		for (String key : authorizationEndpointRequest.keySet()) {

			if (!(requestObjectClaims.get(key) instanceof JsonPrimitive)) {
				// only handle stringable values for now (as BuildPlainRedirectToAuthorizationEndpoint)
				continue;
			}

			String requestObjectValue = requestObjectClaims.get(key).getAsString();
			String requestParameterValue = authorizationEndpointRequest.get(key).getAsString();

			if (REQUIRED_PARAMETERS.contains(key)
					|| requestObjectValue == null
					|| !requestParameterValue.equals(requestObjectValue)) {
				builder.queryParam(key, authorizationEndpointRequest.get(key).getAsString());
			}
		}

		String redirectTo = builder.toUriString();

		logSuccess("Sending to authorization endpoint", args("redirect_to_authorization_endpoint", redirectTo));

		env.putString("redirect_to_authorization_endpoint", redirectTo);

		return env;
	}

}
