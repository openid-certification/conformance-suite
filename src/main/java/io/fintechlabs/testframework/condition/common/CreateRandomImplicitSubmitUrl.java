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

package io.fintechlabs.testframework.condition.common;

import org.apache.commons.lang3.RandomStringUtils;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * Creates a callback URL based on the base_url environment value
 * 
 * 
 * @author jricher
 *
 */
public class CreateRandomImplicitSubmitUrl extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 */
	public CreateRandomImplicitSubmitUrl(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.Condition#assertTrue(io.fintechlabs.testframework.testmodule.Environment, io.fintechlabs.testframework.logging.EventLog)
	 */
	@Override
	@PreEnvironment(strings = "base_url")
	@PostEnvironment(required = "implicit_submit")
	public Environment evaluate(Environment env) {
		String baseUrl = env.getString("base_url");

		if (Strings.isNullOrEmpty(baseUrl)) {
			throw error("Base URL was null or empty");
		}

		// create a random submission URL
		String path = RandomStringUtils.randomAlphanumeric(20);

		JsonObject o = new JsonObject();
		o.addProperty("path", path);
		o.addProperty("fullUrl", baseUrl + "/" + path);

		env.put("implicit_submit", o);

		logSuccess("Created random implicit submission URL",
			args("implicit_submit", o));

		return env;
	}

}
