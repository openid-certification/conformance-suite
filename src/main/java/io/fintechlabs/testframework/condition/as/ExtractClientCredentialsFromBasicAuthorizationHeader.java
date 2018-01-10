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

package io.fintechlabs.testframework.condition.as;

import java.util.Base64;
import java.util.List;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class ExtractClientCredentialsFromBasicAuthorizationHeader extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 * @param conditionResultOnFailure
	 * @param requirements
	 */
	public ExtractClientCredentialsFromBasicAuthorizationHeader(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = "token_endpoint_request")
	@PostEnvironment(required = "client_authentication")
	public Environment evaluate(Environment env) {
		
		if (env.containsObj("client_authentication")) {
			return error("Found existing client authentication");
		}
		
		String auth = env.getString("token_endpoint_request", "headers.authorization");
		
		if (Strings.isNullOrEmpty(auth)) {
			return error("Couldn't find authorization header");
		} 
		
		if (!auth.toLowerCase().startsWith("basic")) {
			return error("Not a basic authorization header", args("auth", auth));
		}
		
		// parse the HTTP Basic Auth
		
		
		String decoded = new String(Base64.getDecoder().decode( // base64 decode
				auth.substring("Basic ".length()))); // strip off the "Basic " prefix first though
		
		List<String> parts = Lists.newArrayList(Splitter.on(":").split(decoded)); // split the results at a colon to get username:password (in our case, clientId:clientSecret)
		
		if (parts.size() != 2) {
			// we don't have two parts
			return error("Unexpected number of parts to authorization header", args("basic_auth", parts));
		}
		
		String clientId = parts.get(0);
		String clientSecret = parts.get(1);
		
		JsonObject clientAuthentication = new JsonObject();
		clientAuthentication.addProperty("client_id", clientId);
		clientAuthentication.addProperty("client_secret", clientSecret);
		clientAuthentication.addProperty("method", "client_secret_basic");
		
		env.put("client_authentication", clientAuthentication);
		
		logSuccess("Extracted client authentication", clientAuthentication);
		
		return env;
		
	}

}
