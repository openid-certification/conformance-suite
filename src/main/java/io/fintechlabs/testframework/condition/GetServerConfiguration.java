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

import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import io.fintechlabs.testframework.logging.EventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class GetServerConfiguration extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 */
	public GetServerConfiguration(String testId, EventLog log) {
		super(testId, log);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment, java.lang.String, io.fintechlabs.testframework.logging.EventLog)
	 */
	@Override
	public Environment evaluate(Environment in) {
		
		if (!in.containsObj("config")) {
			throwError("Couldn't find a configuration");
			return null; // never reached
		}
		
		// get out the server configuration component
		if (!Strings.isNullOrEmpty(in.getString("config", "server.discoveryUrl"))) {
			// do an auto-discovery here
			
			String discoveryUrl = in.getString("config", "server.discoveryUrl");
			
			RestTemplate restTemplate = new RestTemplate();
			
			// TODO: construct the well-known URI if needed from the issuer field
			//String url = issuer + "/.well-known/openid-configuration";

			// fetch the value
			String jsonString;
			try {
				jsonString = restTemplate.getForObject(discoveryUrl, String.class);
			} catch (RestClientResponseException e) {
				throwError("Unable to fetch server configuration from " + discoveryUrl, e);
				return null;
			}

			log(ImmutableMap.of("msg", "Downloaded server configuration", 
					"server_config_string", jsonString));

			if (!Strings.isNullOrEmpty(jsonString)) {
				try {
					JsonObject serverConfig = new JsonParser().parse(jsonString).getAsJsonObject();
					
					log("Successfully parsed server configuration"); // TODO: add in the parsed serverConfig object to the log
					
					in.put("server", serverConfig);
					
					logSuccess();
					return in;
				} catch (JsonSyntaxException e) {
					throwError(e);
					return null; // never reached
				}
				
				
			} else {
				throwError("empty server configuration");
				return null; // never reached
			}
			
		} else {
			// check for manual configuration here
			// TODO!
			throwError("Static configuration not yet implemented");
			return null; // never reached
		}

		
		
	}

}
