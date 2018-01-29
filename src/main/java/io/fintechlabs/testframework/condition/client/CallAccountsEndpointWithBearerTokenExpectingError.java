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

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.Collections;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class CallAccountsEndpointWithBearerTokenExpectingError extends AbstractCondition {

	private static final String ACCOUNTS_RESOURCE = "accounts";

	public CallAccountsEndpointWithBearerTokenExpectingError(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = { "access_token", "resource" })
	@PostEnvironment(required = "resource_endpoint_response_headers", strings = "resource_endpoint_response")
	public Environment evaluate(Environment env) {

		String accessToken = env.getString("access_token", "value");
		if (Strings.isNullOrEmpty(accessToken)) {
			return error("Access token not found");
		}

		String tokenType = env.getString("access_token", "type");
		if (Strings.isNullOrEmpty(tokenType)) {
			return error("Token type not found");
		} else if (!tokenType.equals("Bearer")) {
			return error("Access token is not a bearer token", args("token_type", tokenType));
		}

		String resourceEndpoint = env.getString("resource", "resourceUrl");
		if (Strings.isNullOrEmpty(resourceEndpoint)) {
			return error("Resource endpoint not found");
		}

		JsonObject requestHeaders = env.get("resource_endpoint_request_headers");

		// Build the endpoint URL
		String accountRequestsUrl = UriComponentsBuilder.fromUriString(resourceEndpoint)
			.path(ACCOUNTS_RESOURCE)
			.toUriString();

		try {
			RestTemplate restTemplate = createRestTemplate(env);

			HttpHeaders headers = new HttpHeaders();

			headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON_UTF8));
			headers.setAcceptCharset(Collections.singletonList(Charset.forName("UTF-8")));
			headers.set("Authorization", String.join(" ", tokenType, accessToken));

			if (requestHeaders != null) {
				for (Map.Entry<String, JsonElement> header : requestHeaders.entrySet()) {
					headers.set(header.getKey(), header.getValue().getAsString());
				}
			}

			HttpEntity<?> request = new HttpEntity<>(headers);

			ResponseEntity<String> response = null;

			try {
				response = restTemplate.exchange(accountRequestsUrl, HttpMethod.GET, request, String.class);
			} catch (RestClientResponseException e) {

				logSuccess("Resource endpoint returned error", args("code", e.getRawStatusCode(), "status", e.getStatusText(), "body", e.getResponseBodyAsString()));

				return env;
			}

			String responseBody = response.getBody();

			if (Strings.isNullOrEmpty(responseBody)) {
				return error("Empty response from the resource endpoint");
			} else {
				try {
					JsonElement jsonRoot = new JsonParser().parse(responseBody);
					if (jsonRoot == null || !jsonRoot.isJsonObject()) {
						return error("Resource endpoint did not return a JSON object");
					}

					JsonObject responseObj = jsonRoot.getAsJsonObject();

					if (responseObj.has("error") && !Strings.isNullOrEmpty(responseObj.get("error").getAsString())) {
						logSuccess("Found error in resource endpoint error response", responseObj);
						return env;
					} else {
						return error("No error from resource endpoint", responseObj);
					}

				} catch (JsonParseException e) {
					return error(e);
				}
			}
		} catch (UnrecoverableKeyException | KeyManagementException | CertificateException | InvalidKeySpecException | NoSuchAlgorithmException | KeyStoreException | IOException e) {
			return error("Error creating HTTP client", e);
		}
	}

}
