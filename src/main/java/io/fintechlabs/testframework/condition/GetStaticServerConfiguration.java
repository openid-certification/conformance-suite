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
import com.google.gson.JsonElement;

import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class GetStaticServerConfiguration extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 */
	public GetStaticServerConfiguration(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = "config")
	@PostEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		if (!env.containsObj("config")) {
			return error("Couldn't find a configuration");
		}
		
		String discoveryUrl = env.getString("config", "server.discoveryUrl");
		String iss = env.getString("config", "server.discoveryIssuer");
		
		if (!Strings.isNullOrEmpty(discoveryUrl) || !Strings.isNullOrEmpty(iss)) {
			return error("Dynamic configuration elements found, skipping static configuration", args("discoveryUrl", discoveryUrl, "discoveryIssuer", iss));
		}

		// make sure we've got a server object
		JsonElement server = env.findElement("config", "server");
		if (server == null || !server.isJsonObject()) {
			return error("Couldn't find server object in configuration");
		} else {
			// we've got a server object, put it in the environment
			env.put("server", server.getAsJsonObject());
			
			logSuccess("Found a static server object", server.getAsJsonObject());
			return env;
		}
	}

}
