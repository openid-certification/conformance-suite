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
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class ExtractJWKsFromClientConfiguration extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 * @param optional
	 */
	public ExtractJWKsFromClientConfiguration(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = "client")
	@PostEnvironment(required = "jwks")
	public Environment evaluate(Environment env) {

		if (!env.containsObj("client")) {
			return error("Couldn't find client configuration");
		}
		
		// bump the client's internal JWK up to the root
		JsonElement jwks = env.findElement("client", "jwks");
		
		if (jwks == null) {
			return error("Couldn't find JWKs in client configuration");
		} else if (!(jwks instanceof JsonObject)) {
			return error("Invalid JWKs in client configuration");
		}
		
		logSuccess("Extracted client JWK", args("jwks", jwks));
		
		env.put("jwks", jwks.getAsJsonObject());
		
		return env;
		
	}

}
