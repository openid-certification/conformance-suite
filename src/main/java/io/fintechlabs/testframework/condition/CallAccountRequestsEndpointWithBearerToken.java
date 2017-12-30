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
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class CallAccountRequestsEndpointWithBearerToken extends AbstractCondition {

	private static final String ACCOUNT_REQUESTS_RESOURCE = "account-requests";

	private static final Logger logger = LoggerFactory.getLogger(CallAccountRequestsEndpointWithBearerToken.class);


	/**
	 * @param testId
	 * @param log
	 */
	public CallAccountRequestsEndpointWithBearerToken(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = {"access_token", "resource", "account_requests_endpoint_request"})
	@PostEnvironment(required = "account_requests_endpoint_response")
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

		JsonObject requestObject = env.get("account_requests_endpoint_request");
		if (requestObject == null) {
			return error("Couldn't find request objcet");
		}

		// Build the endpoint URL
		String accountRequestsUrl = UriComponentsBuilder.fromUriString(resourceEndpoint)
				.path(ACCOUNT_REQUESTS_RESOURCE)
				.toUriString();

		try {
			RestTemplate restTemplate = createRestTemplate(env);

			HttpHeaders headers = new HttpHeaders();
			headers.add("Authorization", String.join(" ", tokenType, accessToken));

			HttpEntity<String> request = new HttpEntity<>(requestObject.toString(), headers);

			String jsonString = restTemplate.postForObject(accountRequestsUrl, request, String.class);

			if (Strings.isNullOrEmpty(jsonString)) {
				return error("Didn't get back a response from the account requests endpoint");
			} else {
				log("Account requests endpoint response", args("account_requests_endpoint_response", jsonString));

				try {
					JsonElement jsonRoot = new JsonParser().parse(jsonString);
					if (jsonRoot == null || !jsonRoot.isJsonObject()) {
						return error("Account requests endpoint did not return a JSON object");
					}

					logSuccess("Parsed account requests endpoint response", jsonRoot.getAsJsonObject());

					env.put("account_requests_endpoint_response", jsonRoot.getAsJsonObject());

					return env;
				} catch (JsonParseException e) {
					return error(e);
				}
			}
		} catch (RestClientResponseException e) {
			return error("Error from the account requests endpoint", e, args("code", e.getRawStatusCode(), "status", e.getStatusText(), "body", e.getResponseBodyAsString()));
		} catch (NoSuchAlgorithmException | KeyManagementException | CertificateException | InvalidKeySpecException | KeyStoreException | IOException | UnrecoverableKeyException e) {
			logger.warn("Error creating HTTP Client", e);
			return error("Error creating HTTP Client", e);
		}

	}

}
