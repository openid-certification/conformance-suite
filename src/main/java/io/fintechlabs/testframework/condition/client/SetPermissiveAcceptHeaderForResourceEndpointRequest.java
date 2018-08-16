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

import org.springframework.http.HttpHeaders;

import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class SetPermissiveAcceptHeaderForResourceEndpointRequest extends AbstractCondition {

	public SetPermissiveAcceptHeaderForResourceEndpointRequest(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	public Environment evaluate(Environment env) {

		JsonObject requestHeaders = env.getObject("resource_endpoint_request_headers");

		if (requestHeaders == null) {
			requestHeaders = new JsonObject();
			env.putObject("resource_endpoint_request_headers", requestHeaders);
		}

		requestHeaders.addProperty(HttpHeaders.ACCEPT, "application/json, application/*+json, */*");

		logSuccess("Set Accept header", args("Accept", requestHeaders.get("Accept")));

		return env;
	}

}
