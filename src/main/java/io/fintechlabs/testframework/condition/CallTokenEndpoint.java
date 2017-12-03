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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import io.fintechlabs.testframework.logging.EventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class CallTokenEndpoint extends AbstractCondition {


	private static final Logger logger = LoggerFactory.getLogger(CallTokenEndpoint.class);


	/**
	 * @param testId
	 * @param log
	 */
	public CallTokenEndpoint(String testId, EventLog log, boolean optional) {
		super(testId, log, optional);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = {"server", "token_endpoint_request_form_parameters", "token_endpoint_request_headers"})
	@PostEnvironment(required = "token_endpoint_response")
	public Environment evaluate(Environment env) {

		if (env.getString("server", "token_endpoint") == null) {
			return error("Couldn't find token endpoint");
		}

		if (!env.containsObj("token_endpoint_request_form_parameters")) {
			return error("Couldn't find request form");
		}

		// build up the form
		JsonObject formJson = env.get("token_endpoint_request_form_parameters");
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		for (String key : formJson.keySet()) {
			form.add(key, formJson.get(key).getAsString());
		}


		try {
			RestTemplate restTemplate = createRestTemplate(env);

			// extract the headers for use (below)
			HttpHeaders headers = new HttpHeaders();
			
			JsonObject headersJson = env.get("token_endpoint_request_headers");
			if (headersJson != null) {
				for (String header : headersJson.keySet()) {
					headers.add(header, headersJson.get(header).getAsString());
				}
			}
			
			HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);
			
			String jsonString = null;

			try {
				jsonString = restTemplate.postForObject(env.getString("server", "token_endpoint"), request, String.class);
			} catch (RestClientResponseException e) {

				return error("Error from the token endpoint", e, args("code", e.getRawStatusCode(), "status", e.getStatusText(), "body", e.getResponseBodyAsString()));
			}

			if (Strings.isNullOrEmpty(jsonString)) {
				return error("Didn't get back a response from the token endpoint");
			} else {
				log("Token endpoint response",
						args("token_endpoint_response", jsonString));

				try {
					JsonElement jsonRoot = new JsonParser().parse(jsonString);
					if (jsonRoot == null || !jsonRoot.isJsonObject()) {
						return error("Token Endpoint did not return a JSON object");
					}

					logSuccess("Parsed token endpoint response", jsonRoot.getAsJsonObject());

					env.put("token_endpoint_response", jsonRoot.getAsJsonObject());

					return env;
				} catch (JsonParseException e) {
					return error(e);
				}
			}
		} catch (NoSuchAlgorithmException | KeyManagementException | CertificateException | InvalidKeySpecException | KeyStoreException | IOException | UnrecoverableKeyException e) {
			logger.warn("Error creating HTTP Client", e);
			return error("Error creating HTTP Client", e);
		}
		

	}

}
