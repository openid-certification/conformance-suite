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

import com.google.gson.JsonObject;

import io.fintechlabs.testframework.logging.EventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class CreateTokenEndpointRequestForAuthorizationCodeGrant extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 */
	public CreateTokenEndpointRequestForAuthorizationCodeGrant(String testId, EventLog log, boolean optional) {
		super(testId, log, optional);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	public Environment evaluate(Environment env) {
		
		JsonObject o = new JsonObject();
		o.addProperty("grant_type", "authorization_code");
		o.addProperty("code", env.getString("code"));
		o.addProperty("redirect_uri", env.getString("redirect_uri"));
		
		log(o);
		
		env.put("token_endpoint_request_form_parameters", o);
		
		logSuccess();

		return env;
	}

}
