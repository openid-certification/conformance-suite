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
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.logging.EventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class ExtractBearerAccessTokenFromHeader extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 * @param optional
	 */
	public ExtractBearerAccessTokenFromHeader(String testId, EventLog log, boolean optional) {
		super(testId, log, optional);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = "incoming_request")
	@PostEnvironment(strings = "incoming_access_token")
	public Environment evaluate(Environment env) {

		log("Incoming request headers", (JsonObject) env.findElement("incoming_request", "headers"));
		
		String auth = env.getString("incoming_request", "headers.authorization");
		
		if (!Strings.isNullOrEmpty(auth)) {
			if (auth.toLowerCase().startsWith("bearer")) {
				String incoming = auth.substring("bearer ".length(), auth.length());
				if (!Strings.isNullOrEmpty(incoming)) {
					logSuccess("Found access token on incoming request", args("access_token", incoming));
					env.putString("incoming_access_token", incoming);
					return env;
				} else {
					return error("Couldn't find access token in header");
				}
			} else {
				return error("Couldn't find bearer token in authorization header");
			}
		} else {
			return error("Couldn't find authorization header");
		}
		
	}

}
