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

import org.assertj.core.util.Strings;
import org.springframework.web.client.RestTemplate;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.fintechlabs.testframework.logging.EventLog;
import io.fintechlabs.testframework.testmodule.AbstractCondition;
import io.fintechlabs.testframework.testmodule.ConditionError;
import io.fintechlabs.testframework.testmodule.Environment;
import io.fintechlabs.testframework.testmodule.TestModuleConfiguration;

/**
 * @author jricher
 *
 */
public class GetServerConfiguration extends AbstractCondition {

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment, java.lang.String, io.fintechlabs.testframework.logging.EventLog)
	 */
	@Override
	public Environment evaluate(Environment in, String src, EventLog log) {
		
		if (!in.containsKey("server")) {
			throw new ConditionError(this, "Couldn't find a server configuration");
		}
		
		TestModuleConfiguration conf = new TestModuleConfiguration(in.get("server"));
		
		// get out the server configuration component
		if (!Strings.isNullOrEmpty(conf.getString("discoveryUrl"))) {
			// do an auto-discovery here
			
			String discoveryUrl = conf.getString("discoveryUrl");
			
			RestTemplate restTemplate = new RestTemplate();
			
			// construct the well-known URI
			//String url = issuer + "/.well-known/openid-configuration";

			// fetch the value
			String jsonString = restTemplate.getForObject(discoveryUrl, String.class);
			
			if (!Strings.isNullOrEmpty(jsonString)) {
				JsonObject serverConfig = new JsonParser().parse(jsonString).getAsJsonObject();
				
				// get the server information  that we were passed in
				JsonObject passedIn = in.get("server");
				
				// copy over everything new that we've seen
				for (String key : serverConfig.keySet()) {
					passedIn.add(key, serverConfig.get(key));
				}
				
				in.put("server", passedIn);
				return in;
				
			} else {
				throw new ConditionError(this, "empty server configuration");
			}
			
		} else {
			// check for manual configuration here
			// TODO!
			throw new ConditionError(this, "Static configuration not yet implemented");
		}

		
		
	}

}
