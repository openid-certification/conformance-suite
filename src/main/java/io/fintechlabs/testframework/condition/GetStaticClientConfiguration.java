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

/**
 * @author jricher
 *
 */
public class GetStaticClientConfiguration extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 */
	public GetStaticClientConfiguration(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = "config")
	@PostEnvironment(required = "client", strings = "client_id")
	public Environment evaluate(Environment in) {

		if (!in.containsObj("config")) {
			return error("Couldn't find a configuration");
		}

		// make sure we've got a client object
		JsonElement client = in.findElement("config", "client");
		if (client == null || !client.isJsonObject()) {
			return error("Couldn't find client object in configuration");
		} else {
			// we've got a client object, put it in the environment
			in.put("client", client.getAsJsonObject());
			
			// pull out the client ID and put it in the root environment for easy access
			in.putString("client_id", in.getString("client", "client_id"));
			
			logSuccess("Found a static client object", client.getAsJsonObject());
			return in;
		}
		
		
	}

}
