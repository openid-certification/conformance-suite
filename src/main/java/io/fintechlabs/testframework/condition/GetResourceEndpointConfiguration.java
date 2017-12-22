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

import com.google.gson.JsonElement;

import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class GetResourceEndpointConfiguration extends AbstractCondition {

	public GetResourceEndpointConfiguration(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = "config")
	@PostEnvironment(required = "resource")
	public Environment evaluate(Environment env) {

		JsonElement resource = env.findElement("config", "resource");
		if (resource == null || !resource.isJsonObject()) {
			return error("Couldn't find resource endpoint object in configuration");
		} else {
			env.put("resource", resource.getAsJsonObject());

			logSuccess("Found a resource endpoint object", resource.getAsJsonObject());
			return env;
		}
	}

}
