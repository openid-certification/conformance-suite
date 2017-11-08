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

import org.springframework.web.util.UriComponentsBuilder;

import io.fintechlabs.testframework.logging.EventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class BuildPlainRedirectToAuthorizationEndpointHybridCodeIdtoken extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 * @param optional
	 */
	public BuildPlainRedirectToAuthorizationEndpointHybridCodeIdtoken(String testId, EventLog log, boolean optional) {
		super(testId, log, optional);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	public Environment evaluate(Environment env) {
		String redirectTo = UriComponentsBuilder.fromHttpUrl(env.getString("server", "authorization_endpoint"))
				.queryParam("client_id", env.getString("client_id"))
				.queryParam("response_type", "code id_token")
				.queryParam("state", env.getString("state"))
				.queryParam("redirect_uri", env.getString("redirect_uri"))
				.queryParam("scope", env.getString("client", "scope"))
				.build().toUriString();

		logSuccess("Sending to authorization endpoint", 
				args("redirect_to_authorization_endpoint", redirectTo));
		
		env.putString("redirect_to_authorization_endpoint", redirectTo);
		
		return env;
	}

}
