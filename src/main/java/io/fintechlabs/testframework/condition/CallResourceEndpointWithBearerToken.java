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
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;

import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class CallResourceEndpointWithBearerToken extends AbstractCondition {

	public CallResourceEndpointWithBearerToken(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = {"access_token", "config"})
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

		String resourceEndpoint = env.getString("config", "resourceUrl");
		if (Strings.isNullOrEmpty(resourceEndpoint)) {
			return error("Resource endpoint not found");
		}

		try {
			RestTemplate restTemplate = createRestTemplate(env);

			HttpHeaders headers = new HttpHeaders();
			headers.add("Authorization", String.join(" ", tokenType, accessToken));

			HttpEntity<String> request = new HttpEntity<String>("parameters", headers);

			ResponseEntity<String> response = restTemplate.exchange(resourceEndpoint, HttpMethod.GET, request, String.class);

			String responseBody = response.getBody();
			JsonObject responseHeaders = new JsonObject();

			for (Map.Entry<String, String> entry : response.getHeaders().toSingleValueMap().entrySet()) {
				responseHeaders.addProperty(entry.getKey(), entry.getValue());
			}

			env.putString("resource_endpoint_response", responseBody);
			env.put("resource_endpoint_response_headers", responseHeaders);

			logSuccess("Got a response from the resource endpoint", args("body", responseBody, "headers", responseHeaders));

			return env;
		} catch (RestClientResponseException e) {
			return error("Error from the resource endpoint", e, args("code", e.getRawStatusCode(), "status", e.getStatusText()));
		} catch (UnrecoverableKeyException | KeyManagementException | CertificateException | InvalidKeySpecException | NoSuchAlgorithmException | KeyStoreException | IOException e) {
			return error("Error creating HTTP client", e);
		}
	}

}
