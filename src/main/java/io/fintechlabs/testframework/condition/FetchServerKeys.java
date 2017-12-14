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
import java.text.ParseException;

import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.jwk.JWKSet;

import io.fintechlabs.testframework.logging.TestInstanceEventLog;
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
	public FetchServerKeys(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = "server")
	@PostEnvironment(required = "server_jwks")
	public Environment evaluate(Environment env) {
		
		if (!env.containsObj("server")) {
			return error("No server configuration found");
		}
		
		JsonElement jwks = env.findElement("server", "jwks");
		
		if (jwks != null && jwks.isJsonObject()) {
			env.put("server_jwks", jwks.getAsJsonObject());
			logSuccess("Found static server JWKS", args("jwks", jwks));
			return env;
		} else {
			// we don't have a key yet, see if we can fetch it
			
			String jwksUri = env.getString("server", "jwks_uri");
			
			if (!Strings.isNullOrEmpty(jwksUri)) {
				// do the fetch

				log("Fetching server key", args("jwks_uri", jwksUri));
				
				try {
					RestTemplate restTemplate = createRestTemplate(env);

					String jwkString = restTemplate.getForObject(jwksUri, String.class);
					
					log("Found JWK set string", args("jwk_string", jwkString));
					
					// parse the key to make sure it's really a JWK
					JWKSet.parse(jwkString);
					
					// since it parsed, we store it as a JSON object to grab it later on
					JsonObject jwkSet = new JsonParser().parse(jwkString).getAsJsonObject();
					env.put("server_jwks", jwkSet);
					
					logSuccess("Parsed server JWK", args("jwk", jwkSet));
					return env;
					
				} catch (UnrecoverableKeyException | KeyManagementException | CertificateException | InvalidKeySpecException | NoSuchAlgorithmException | KeyStoreException | IOException e) {
					return error("Error creating HTTP client", e);
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
