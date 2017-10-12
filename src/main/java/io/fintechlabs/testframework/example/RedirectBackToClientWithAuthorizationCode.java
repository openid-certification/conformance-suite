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

package io.fintechlabs.testframework.example;

import org.springframework.web.util.UriComponentsBuilder;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.logging.EventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class RedirectBackToClientWithAuthorizationCode extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 * @param optional
	 */
	public RedirectBackToClientWithAuthorizationCode(String testId, EventLog log, boolean optional) {
		super(testId, log, optional);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	public Environment evaluate(Environment env) {

		String redirectUri = env.getString("authorization_endpoint_request", "redirect_uri");
		String code = env.getString("authorization_code");
		String state = env.getString("authorization_endpoint_request", "state");
		
		
		String redirectTo = UriComponentsBuilder.fromHttpUrl(redirectUri)
				.queryParam("state", state)
				.queryParam("code", code)
				.toUriString();

		log("Redirecting back to client", args("uri", redirectTo));
		
		logSuccess();
		
		env.putString("authorization_endpoint_response_redirect", redirectTo);
		
		return env;
		
	}

}
