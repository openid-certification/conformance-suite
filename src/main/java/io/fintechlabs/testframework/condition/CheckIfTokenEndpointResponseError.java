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
import com.google.common.collect.ImmutableMap;

import io.fintechlabs.testframework.logging.EventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class CheckIfTokenEndpointResponseError extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 */
	public CheckIfTokenEndpointResponseError(String testId, EventLog log) {
		super(testId, log);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	public Environment evaluate(Environment env) {
		
		if (!env.containsObj("token_endpoint_response")) {
			throwError("Couldn't find token endpoint response");
			return null;
		}

		if (!Strings.isNullOrEmpty(env.getString("token_endpoint_response", "error"))) {
			log(ImmutableMap.of("msg", "Token endpoint error response", 
					"error", env.getString("token_endpoint_response", "error"), 
					"error_description", env.getString("token_endpoint_response", "error_description"),
					"error_uri", env.getString("token_endpoint_response", "error_uri")));
			throwError("Token endpoint error response: " + env.getString("token_endpoint_response", "error"));
			return null;
		} else {
			logSuccess();
			return env;
		}
		

	}

}
