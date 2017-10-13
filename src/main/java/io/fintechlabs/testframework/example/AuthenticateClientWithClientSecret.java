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


import com.google.common.base.Strings;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.logging.EventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class AuthenticateClientWithClientSecret extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 * @param optional
	 */
	public AuthenticateClientWithClientSecret(String testId, EventLog log, boolean optional) {
		super(testId, log, optional);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	public Environment evaluate(Environment env) {
		if (env.containsObj("client_authentication_success")) {
			return error("Found existing client authentication");
		}
		
		if (!env.containsObj("client_authentication")) {
			return error("Couldn't find client authentication");
		}
		
		if (Strings.isNullOrEmpty(env.getString("client_authentication", "method"))) {
			return error("Couldn't determine client authentication method");
		}
		
		if (env.getString("client_authentication", "method").equals("client_secret_post") 
				|| env.getString("client_authentication", "method").equals("client_secret_basic")) {
			
			String expected = env.getString("client", "client_secret");
			String actual = env.getString("client_authentication", "client_secret");
			
			if (!Strings.isNullOrEmpty(expected) 
					&& expected.equals(actual)) {
				
				logSuccess();
				
				env.putString("client_authentication_success", env.getString("client_authentication", "method"));
				
				return env;
				
			} else {
				
				log("Mismatch client secrets", args("expected", expected, "actual", actual));
				
				return error("Unable to authenticate client from secret");
			}
			
		} else {
			return error("Can't handle client method " + env.getString("client_authentication", "method"));
		}
	
	}

}
