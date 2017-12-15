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

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;

import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class GetDynamicServerConfiguration extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 */
	public GetDynamicServerConfiguration(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.testmodule.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment, java.lang.String, io.fintechlabs.testframework.logging.EventLog)
	 */
	@Override
	@PreEnvironment(required = "config")
	@PostEnvironment(required = "server")
	public Environment evaluate(Environment env) {
		
		if (!env.containsObj("config")) {
			return error("Couldn't find a configuration");
		}
		
		String staticIssuer = env.getString("config", "server.issuer");
		
		if (!Strings.isNullOrEmpty(staticIssuer)) {
			return error("Static configuration element found, skipping dynamic server discovery", args("issuer", staticIssuer));
		}
		
		String discoveryUrl = env.getString("config", "server.discoveryUrl");
		
		if (Strings.isNullOrEmpty(discoveryUrl)) {

			String iss = env.getString("config", "server.discoveryIssuer");
			discoveryUrl = iss + "/.well-known/openid-configuration";
			
			if (Strings.isNullOrEmpty(iss)) {
				return error("Couldn't find discoveryUrl or discoveryIssuer field for discovery purposes");
			}
			
		}
		
		// get out the server configuration component
		if (!Strings.isNullOrEmpty(discoveryUrl)) {
			// do an auto-discovery here
			

			// fetch the value
			String jsonString;
			try {
				RestTemplate restTemplate = createRestTemplate(env);
				jsonString = restTemplate.getForObject(discoveryUrl, String.class);
			} catch (UnrecoverableKeyException | KeyManagementException | CertificateException | InvalidKeySpecException | NoSuchAlgorithmException | KeyStoreException | IOException e) {
				return error("Error creating HTTP client", e);
			} catch (RestClientResponseException e) {
				return error("Unable to fetch server configuration from " + discoveryUrl, e);
			}

			if (!Strings.isNullOrEmpty(jsonString)) {
				log("Downloaded server configuration", 
						args("server_config_string", jsonString));

				try {
					JsonObject serverConfig = new JsonParser().parse(jsonString).getAsJsonObject();
					
					logSuccess("Successfully parsed server configuration", serverConfig);
					
					env.put("server", serverConfig);
					
					return env;
				} catch (JsonSyntaxException e) {
					return error(e);
				}
				
				
			} else {
				return error("empty server configuration");
			}
			
		} else {
			return error("Couldn't find or construct a discovery URL");
		}

		
		
	}

}
