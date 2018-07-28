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
import com.google.gson.JsonPrimitive;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class ValidateExpiresIn extends AbstractCondition {

	public ValidateExpiresIn(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PreEnvironment(strings = {}, required = "expires_in")
	public Environment evaluate(Environment env) {

		JsonObject expiresIn = env.get("expires_in");
		JsonElement je = expiresIn.get("expires_in");
		try {
			JsonPrimitive jp = je.getAsJsonPrimitive();
			if (!jp.isNumber()) {
				logFailure(expiresIn);
				throw error("expires_in, is not a Number!");
			}

		} catch (IllegalStateException ex) {
			logFailure(expiresIn);
			throw error("expires_in, is not a primitive!");
		}

		logSuccess("expires_in passed all validation checks",expiresIn);
		return env;

	}

}
