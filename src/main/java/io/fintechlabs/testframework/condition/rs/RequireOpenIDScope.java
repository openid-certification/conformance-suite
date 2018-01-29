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

package io.fintechlabs.testframework.condition.rs;

import java.util.List;

import com.google.common.base.Splitter;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;
import jersey.repackaged.com.google.common.collect.Lists;

/**
 * @author jricher
 *
 */
public class RequireOpenIDScope extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 * @param optional
	 */
	public RequireOpenIDScope(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(strings = "scope")
	public Environment evaluate(Environment env) {
		String scope = env.getString("scope");

		List<String> scopes = Lists.newArrayList(Splitter.on(" ").split(scope));

		if (!scopes.contains("openid")) {
			return error("Couldn't find openid scope", args("scopes", scopes));
		} else {
			logSuccess("Found openid scope in scopes list", args("scopes", scopes));
			return env;
		}
	}

}
