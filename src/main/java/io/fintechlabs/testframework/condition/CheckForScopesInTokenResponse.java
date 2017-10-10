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

import java.util.Set;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import io.fintechlabs.testframework.logging.EventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class CheckForScopesInTokenResponse extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 */
	public CheckForScopesInTokenResponse(String testId, EventLog log, boolean optional) {
		super(testId, log, optional, "FAPI-1-5.2.2-15");
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	public Environment evaluate(Environment env) {
		if (!Strings.isNullOrEmpty(env.getString("token_endpoint_response", "scope"))) {
			log(ImmutableMap.of("msg", "Found scopes returned with access token",
					"scope", env.getString("token_endpoint_response", "scope")));
			logSuccess();
			return env;
		} else {
			return error("Couldn't find scope");
		}
	}

}
