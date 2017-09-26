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

import com.google.common.collect.ImmutableMap;

import io.fintechlabs.testframework.logging.EventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class BuildPlainRedirectToAuthorizationEndpoint extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 */
	public BuildPlainRedirectToAuthorizationEndpoint(String testId, EventLog log) {
		super(testId, log);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	public Environment evaluate(Environment in) {
		// send a front channel request to start things off
		String redirectTo = UriComponentsBuilder.fromHttpUrl(in.getString("server", "authorization_endpoint"))
				.queryParam("client_id", in.getString("client_id"))
				.queryParam("response_type", "code")
				.queryParam("state", in.getString("state"))
				.queryParam("redirect_uri", in.getString("redirect_uri"))
				.queryParam("scope", in.getString("client", "scope"))
				.build().toUriString();

		log(ImmutableMap.of("msg", "Sending to authorization endpoint", 
				"redirect_to_authorization_endpoint", redirectTo));
		
		in.putString("redirect_to_authorization_endpoint", redirectTo);
		
		logSuccess();
		
		return in;
	}

}
