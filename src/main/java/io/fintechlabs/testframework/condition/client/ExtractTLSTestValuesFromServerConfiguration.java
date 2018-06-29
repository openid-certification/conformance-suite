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

package io.fintechlabs.testframework.condition.client;

import java.net.MalformedURLException;
import java.net.URL;

import com.google.common.base.Strings;
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
public class ExtractTLSTestValuesFromServerConfiguration extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 * @param conditionResultOnFailure
	 * @param requirements
	 */
	public ExtractTLSTestValuesFromServerConfiguration(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = "server")
	@PostEnvironment(required = {"authorization_endpoint_tls", "token_endpoint_tls"}) // these two are required, others are added as found: userinfo_endpoint_tls, registration_endpoint_tls
	public Environment evaluate(Environment env) {

		String authorizationEndpoint = env.getString("server", "authorization_endpoint");
		if (Strings.isNullOrEmpty(authorizationEndpoint)) {
			throw error("Authorization endpoint not found");
		}
		
		JsonObject authorizationEndpointTls = extractTlsFromUrl(authorizationEndpoint);
		env.put("authorization_endpoint_tls", authorizationEndpointTls);
		
		String tokenEndpoint = env.getString("server", "token_endpoint");
		if (Strings.isNullOrEmpty(tokenEndpoint)) {
			throw error("Token endpoint not found");
		}
		
		JsonObject tokenEndpointTls = extractTlsFromUrl(tokenEndpoint);
		env.put("token_endpoint_tls", tokenEndpointTls);
		
		String userInfoEndpoint = env.getString("server", "userinfo_endpoint");
		JsonObject userInfoEndpointTls = null;
		if (!Strings.isNullOrEmpty(userInfoEndpoint)) {
			userInfoEndpointTls = extractTlsFromUrl(userInfoEndpoint);
			env.put("userinfo_endpoint_tls", userInfoEndpointTls);
		}
		
		String registrationEndpoint = env.getString("server", "registration_endpoint");
		JsonObject registrationEndpointTls = null;
		if (!Strings.isNullOrEmpty(registrationEndpoint)) {
			registrationEndpointTls = extractTlsFromUrl(registrationEndpoint);
			env.put("registration_endpoint_tls", registrationEndpointTls);
		}
		
		logSuccess("Extracted TLS information from authorization server configuration", args(
				"authorization_endpoint", authorizationEndpointTls,
				"token_endpoint", tokenEndpointTls,
				"userinfo_endpoint", userInfoEndpointTls,
				"registration_endpoint", registrationEndpointTls
			));

		return env;
	}

	private JsonObject extractTlsFromUrl(String urlString) {
		try {
			URL url = new URL(urlString);
			JsonObject tls = new JsonObject();
			tls.addProperty("testHost", url.getHost());
			tls.addProperty("testPort", url.getPort() > 0 ? url.getPort() : url.getDefaultPort());
			
			return tls;
		} catch (MalformedURLException e) {
			throw error("URL not properly formed", e, args("url", urlString));
		}
	}
	
}
