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
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;

import org.apache.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.common.base.Strings;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class DisallowAccessTokenInQuery extends AbstractCondition {

	private static final String ACCOUNTS_RESOURCE = "accounts";

	public DisallowAccessTokenInQuery(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = {"access_token", "resource"})
	public Environment evaluate(Environment env) {

		String accessToken = env.getString("access_token", "value");
		if (Strings.isNullOrEmpty(accessToken)) {
			return error("Access token not found");
		}

		String resourceEndpoint = env.getString("resource", "resourceUrl");
		if (Strings.isNullOrEmpty(resourceEndpoint)) {
			return error("Resource endpoint not found");
		}

		// Build the endpoint URL
		String accountRequestsUrl = UriComponentsBuilder.fromUriString(resourceEndpoint)
				.path(ACCOUNTS_RESOURCE)
				.toUriString();

		try {
			RestTemplate restTemplate = createRestTemplate(env);

			UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(accountRequestsUrl);
			builder.queryParam("access_token", accessToken);

			ResponseEntity<String> response = restTemplate.exchange(builder.toUriString(), HttpMethod.GET, null, String.class);

			return error("Got a successful response from the resource endpoint", args("body", response.getBody()));
		} catch (RestClientResponseException e) {
			if (e.getRawStatusCode() == HttpStatus.SC_BAD_REQUEST || e.getRawStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
				logSuccess("Resource server refused request", args("code", e.getRawStatusCode(), "status", e.getStatusText()));
				return env;
			} else {
				return error("Error from the resource endpoint", e, args("code", e.getRawStatusCode(), "status", e.getStatusText()));
			}
		} catch (UnrecoverableKeyException | KeyManagementException | CertificateException | InvalidKeySpecException | NoSuchAlgorithmException | KeyStoreException | IOException e) {
			return error("Error creating HTTP client", e);
		}
	}

}
