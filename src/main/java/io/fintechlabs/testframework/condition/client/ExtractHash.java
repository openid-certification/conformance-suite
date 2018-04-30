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

import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author ddrysdale
 *
 */
public abstract class ExtractHash extends AbstractCondition {

	protected String HashName; 
	protected String EnvName;
	
	/**
	 * @param testId
	 * @param log
	 * @param conditionResultOnFailure
	 * @param requirements
	 */
	public ExtractHash(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}
	
	@Override
	public Environment evaluate(Environment env) {
		
		env.remove(EnvName);

		if (!env.containsObj("id_token")) {
			throw error("Couldn't find parsed ID token");
		}

		String hash = env.getString("id_token", "claims." + HashName);
		if (hash == null) {
			throw error("Couldn't find " + HashName + " in ID token");
		}

		String alg = env.getString("id_token", "header.alg");
		if (alg == null) {
			throw error("Couldn't find algorithm in ID token header");
		}

		JsonObject outData = new JsonObject();

		outData.addProperty(HashName, hash);
		outData.addProperty("alg", alg);

		env.put(EnvName, outData);

		logSuccess("Extracted " + HashName + " from ID Token", outData);
		
		return env;
	}

}
