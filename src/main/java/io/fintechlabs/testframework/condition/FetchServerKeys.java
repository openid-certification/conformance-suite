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

import java.text.ParseException;

import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.jwk.JWKSet;

import io.fintechlabs.testframework.logging.EventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class FetchServerKeys extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 */
	public FetchServerKeys(String testId, EventLog log) {
		super(testId, log);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	public Environment evaluate(Environment env) {
		
		if (!env.containsObj("server")) {
			return error("No server configuration found");
		}
		
		JsonElement jwks = env.findElement("server", "jwks");
		
		if (jwks != null && jwks.isJsonObject()) {
			log("Found static server JWKS", ImmutableMap.of("jwks", jwks));
			env.put("server_jwks", jwks.getAsJsonObject());
			logSuccess();
			return env;
		} else {
			// we don't have a key yet, see if we can fetch it
			
			String jwksUri = env.getString("server", "jwks_uri");
			
			if (!Strings.isNullOrEmpty(jwksUri)) {
				// do the fetch
				RestTemplate restTemplate = new RestTemplate();

				log("Fetching server key", ImmutableMap.of("jwks_uri", jwksUri));
				
				try {
					String jwkString = restTemplate.getForObject(jwksUri, String.class);
					
					log("Found JWK set string", ImmutableMap.of("jwk_string", jwkString));
					
					// parse the key to make sure it's really a JWK
					JWKSet.parse(jwkString);
					
					// since it parsed, we store it as a JSON object to grab it later on
					JsonObject jwkSet = new JsonParser().parse(jwkString).getAsJsonObject();
					env.put("server_jwks", jwkSet);
					
					logSuccess();
					return env;
					
				} catch (RestClientException e) {
					return error("Exception while fetching server key", e);
				} catch (ParseException e) {
					return error("Unable to parse jwk set", e);
				}
				
			} else {
				return error("Didn't find a JWKS or a JWKS URI in the server configuration");
			}
			
		}
		
		
	}

}
