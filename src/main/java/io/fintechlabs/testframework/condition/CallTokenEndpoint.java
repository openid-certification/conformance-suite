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
import java.net.URI;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.fintechlabs.testframework.logging.EventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author jricher
 *
 */
public class CallTokenEndpoint extends AbstractCondition {

	/**
	 * @param testId
	 * @param log
	 */
	public CallTokenEndpoint(String testId, EventLog log) {
		super(testId, log);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	public Environment evaluate(Environment env) {

		if (!env.containsObj("token_endpoint_request_form_parameters")) {
			throwError("Couldn't find request form");
			return null;
		}

		// build up the form
		JsonObject formJson = env.get("token_endpoint_request_form_parameters");
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		for (String key : formJson.keySet()) {
			form.add(key, formJson.get(key).getAsString());
		}
		
		// extract the headers for use (below)
		final JsonObject headersJson = env.get("token_endpoint_request_headers");
		
		HttpClient httpClient = HttpClientBuilder.create()
				.useSystemProperties()
				.build();

		HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
		
		RestTemplate restTemplate = new RestTemplate(factory){

			@Override
			protected ClientHttpRequest createRequest(URI url, HttpMethod method) throws IOException {
				ClientHttpRequest httpRequest = super.createRequest(url, method);
				if (headersJson != null) {
					// add all the headers
					for (String key : headersJson.keySet()) {
						httpRequest.getHeaders().add(key, headersJson.get(key).getAsString());
					}
				}

				return httpRequest;
			}
		};
		
		String jsonString = null;

		try {
			jsonString = restTemplate.postForObject(env.getString("server", "token_endpoint"), form, String.class);
		} catch (RestClientException e) {

			log("Token endpoint error: " + e.getMessage());

			throwError("Error from the token endpoint", e);
			return null;
		}
		
		if (Strings.isNullOrEmpty(jsonString)) {
			throwError("Didn't get back a response from the token endpoint");
			return null;
		} else {
			log(ImmutableMap.of("msg", "Token endpoint response", "token_endpoint_response", jsonString));
			
			JsonElement jsonRoot = new JsonParser().parse(jsonString);
			if (jsonRoot == null || !jsonRoot.isJsonObject()) {
				throwError("Token Endpoint did not return a JSON object");
				return null;
			}

			env.put("token_endpoint_response", jsonRoot.getAsJsonObject());
			
			logSuccess();
			return env;
		}
		
	}

}
